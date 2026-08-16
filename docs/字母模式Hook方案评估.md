# 字母模式 Hook 方案评估（基于微信输入法 3.5.3 反编译）

> 分析对象：com.tencent.wetype 3.5.3（jadx 反编译，包名/类名部分混淆）。
> 结论：现有"取消按键 + InputConnection 直接提交"的 Hook 方案可行，本次修正三处兼容与语义问题后落地。

## 1. 输入链路（jadx 还原）

1. 键盘视图基类 `com.tencent.wetype.plugin.hld.keyboard.selfdraw.n`（KeyboardView），
   触摸都在 `onTouch(View, MotionEvent)` 中处理（该方法指令较多，反编译不完整，但方法签名、
   按键识别方法均稳定存在）。`selfdraw.n` 未重写 dispatchTouchEvent，`onTouchEvent`
   直接透传 super，因此模块 Hook `View.dispatchTouchEvent` 能在按键处理前拦截到全部触摸。
2. 按键识别：`x1(MotionEvent, boolean)` → 按键对象 `selfdraw.j`（3.5.3 混淆名；
   早期版本为 `v1(MotionEvent, boolean)`），另有 `B1(int,int,boolean,boolean)` 按坐标取键。
3. 按键派发：键盘通过 `getMKeyboardActionListener()`（`va.InterfaceC4188f`，唯一抽象方法
   `a(int)`）把按键抛给上层；英文键盘 S12EnglishNumberSymbolKeyboard 等都在调用它。
4. 输入法与候选：
   - `WxHldService`（InputMethodService 子类）持有 `setComposingText`、`finishComposingText`
     （内部 `y1(true)`）、以及 `C/D/a0/t1/v1(CharSequence..., va.a)` 等提交入口。
   - 引擎/模型 `com.tencent.wetype.plugin.hld.model.i0` 把候选列表喂给
     `candidate.ImeCandidateView.h/w/o`，`o(PendingInput[], CharSequence, ...)` 负责
     组合输入（下划线文本）展示。
   - 英文输入时点字母 → 键盘抛键给引擎 → 引擎生成候选 → 候选栏显示：这就是用户反馈的"点字母出现候选字"。

## 2. 现有字母模式实现原理（仓库已提交版本，3.5.3 重复输入修复后）

- DOWN 字母键：先用反射按触摸坐标识别按下的键（a–z 或空格），**不把真实 DOWN 交给键盘**，
  只向键盘视图派发 ACTION_CANCEL——键盘从未进入按下态，不会自己派发字母/点击，
  从根源避免"点一个输入两个"；引擎收不到按键，自然不生成候选词。
- UP（普通点击）：模块自己通过 `InputConnection.commitText(字母, 1)` 直接上屏，
  绕开输入法引擎；若触发下滑手势则仍执行下滑动作。
- 非字母模式（含手势绑定键）保持原生：真实 DOWN 交给键盘，普通单击正常输入，
  仅下滑时由模块接管执行动作。

> 3.5.3 重复输入根因：早期实现先向键盘派发真实 DOWN 再发 ACTION_CANCEL，键盘在
> "有按下态的 CANCEL"下仍会派发一次按键（引擎输入一个字母），UP 时模块又 commitText
> 一个字母，合成两个。修复后键盘完全看不到真实 DOWN。

对照 3.5.3 代码，该方案成立的关键前提都满足：
- 所有自绘键盘（含英文键盘 S3EnglishQwertyKeyboard / S12EnglishNumberSymbolKeyboard）
  都继承 `selfdraw.n`，模块按基类名匹配即可覆盖，不依赖具体子类；
- 取消触摸后无 `va.f.a(int)` 派发，引擎/候选栏链路完全不进入；
- `commitText` 走平台标准输入通道，不经过输入法组词引擎。

## 3. 本次修正的问题

| 问题 | 原状 | 修正 |
| --- | --- | --- |
| 3.5.3 按键识别改名 | 只认 `v1(MotionEvent, boolean)`，3.5.3 已改名 `x1`，按键识别会失败，字母模式静默失效 | keyAfterDown 按 `x1 → v1` 顺序查找，并用 `B1(x,y)` 按坐标兜底 |
| 影响中文输入 | 字母模式对所有 26 键生效，中文拼音/双拼键盘的字母也会逐字上屏，中文无法组词 | 新增 LetterModeGuard：类名含 English（或 KeyboardType 枚举为 EnglishQwerty/EnglishNumberSymbols）才生效 |
| 残留候选字 | 只清理平台 composing，输入法内部 PendingInput/候选栏可能残留上次候选 | 提交前调用输入法 `finishComposingText()`（WxHldService 覆写 → y1(true) 清理 PendingInput + k1 刷新候选 UI） |

## 4. 风险与边界

- 反射匹配失败一律 try/catch 兜底，不影响输入法崩溃（ExceptionMode.PROTECTIVE）。
- 字母模式接管期间键盘无按下态：26 键逐键点击只出一个字母；长按/滑行输入在字母模式下不生效（符合该模式语义）。
- 开启字母模式后，绑定了下滑动作的字母键（如 V=粘贴）单击会直接上屏字母，下滑仍执行动作。
- 无法识别键盘类型时退回旧行为（对全部字母键生效），保底英文功能不丢失。
- 暂不支持大小写映射（大写锁定下仍提交小写字母），后续如需可在键盘 upperMode 语义明确后补充。
- 候选栏等内部实现随版本变化，兼容基线以 README 为准；真机验收项见第 5 节。

## 5. 真机验收步骤

1. Android 16 + 微信输入法 3.5.3，LSPosed 启用模块并勾选作用域。
2. 模块设置开启"字母模式"，保存并重启输入法进程。
3. 英文键盘 26 键逐键点击 a–z：每点一次只上屏一个字母、候选栏不出现；空格直接上空格（重点验证"点一个输入两个"已修复）。
4. 切到中文拼音键盘输入：组词、候选、选词行为与未开启时完全一致。
5. 英文键盘长按/滑动输入与下滑快捷动作正常；下拉触发不受影响。
6. 开启字母模式后直接输入已有组词上下文，候选栏不残留上一次候选。
7. 竖屏/横屏/26 键/九宫格各验证一轮；关闭字母模式后行为恢复原状。