# 版本记录

## 1.7.1 - 2026-04-29

- 撤回 1.7.0 中在 Activity 启动裁决前同步查询规则 provider 的实现，避免开机后首次打开应用时可能阻塞 system_server 并触发异常重启。
- system_server 中规则预热和规则更新广播重新改为只请求后台异步刷新，不在 ActivityStarter hook 或广播回调里等待 provider 查询完成。
- provider 查询失败时不再把失败时间当作有效缓存时间，后续 Activity 启动会继续请求后台刷新，降低缓存长时间停留在空规则的概率。

## 1.7.0 - 2026-04-29

- 修复 system_server 中规则缓存首次加载为异步刷新时，可能先用空缓存裁决，导致 `!disagree A *` 等规则在刚重启、刚升级或缓存未初始化时短暂不生效的问题。
- `loadRulesFromProvider()` 在缓存尚未初始化时改为同步读取 provider，成功后再返回规则列表，避免首次 Activity 启动被旧缓存或空规则放行。
- system_server 规则预热改为在后台线程中同步完成规则和设置刷新，减少首次使用时的缓存竞态。

## 1.6.9 - 2026-04-26

- 新增 `!` 高权重规则：`!agree`、`!disagree`、`!ask` 会优先于普通规则执行，同一权重内仍按原有顺序规则处理。
- 新增目标排除语法：`agree A *|B` 表示 `A` 可拉起任意非系统目标，但不包括 `B`；排除只作用于目标侧。
- 冲突检查改为只统计同权重下的不同动作重叠，使用 `!` 主动提升权重的规则不再反复报普通冲突。
- 更新规则格式化、无效行检查、说明文案和单元测试，覆盖高权重、目标排除和兼容普通 `ask` 的行为。
- README 新增 `1.6.9` 界面截图展示，并补充与 `Activity链式启动管理器` 的公开信息对比和参考说明。

## 1.6.8 - 2026-04-26

- 新增两段式参与方规则：`agree A`、`disagree A`、`ask A`，用于匹配 `A` 拉起任意非系统应用或任意非系统应用拉起 `A`。
- 规则格式化、无效行检查、说明文案和单元测试同步支持两段式规则。
- 修复规则编辑区右侧空白无法像普通编辑框一样点按定位的问题，同时保留底部空白区不误选中最后一行文本。

## 1.6.7 - 2026-04-25

- 新增 `GPL-3.0-only` 许可证文件，并在 README 中标明项目许可证。
- 将规则中的 `*` 调整为仅匹配非系统应用，避免 `ask * *` 默认接管系统应用或系统组件之间的 Activity 启动。
- 保留 `system` 显式规则别名；需要调查系统拉起时可使用 `ask system *` 或 `ask * system`。
- 更新规则说明和单元测试，覆盖通配符与系统应用的匹配边界。

## 1.6.6 - 2026-04-25

- 做溯源清理：移除未使用的 `ReflectUtil` 和 `ResourceUtil`，避免继续保留早期模板工具类。
- 重写 `LogUtil` 日志工具实现，保持 system_server 中不写文件，降低清理过程引入系统稳定性风险。
- README 新增对 `MagicianGuo/Android-XposedTest` 的致谢，说明早期构建和开发参考了其项目结构与 Xposed 模块示例思路。
- Gradle 子模块和发布文档中的 APK/源码归档名改为当前项目名，减少旧项目命名残留。

## 1.6.5 - 2026-04-25

- 准备公开发布所需资料：新增 `SUMMARY`，重写 `README.md`，补充 Xposed Modules Repo 提交流程说明。
- 调整签名配置：debug 继续使用本地测试签名，release 优先读取本地 `release-signing.properties` 和 `local-signing/` 中的发布签名。
- 更新发布检查清单和发布产物规则，明确 release APK、SHA256 校验文件、源码归档和 `VERSION_CODE-VERSION_NAME` tag 要求。

## 1.6.4 - 2026-04-25

- 修复规则编辑框底部空白区单击、双击或长按仍会影响最后一行文本的问题：空白区手势不再进入 `EditText` 默认文本定位流程。
- 保留文本本体点击、长按和双击选择能力，并继续保留空白区双指缩放。
- 移除 `RuleEditText` 中已弃用的两参数 `performLongClick` 覆盖，减少后续编译警告。

## 1.6.3 - 2026-04-25

- 修复规则编辑框空白区域长按会选中最近文本的问题：只有按下点落在实际文本行且位于文字宽度范围内时，才允许触发长按选中文本。
- 底部空白区、右侧空白区和行号区域长按不再选中规则文本，同时不影响双指缩放。

## 1.6.2 - 2026-04-25

- 规则编辑区外层横向滚动区域新增双指缩放监听，在文本右侧和下方空白区域也可以缩放规则字号。
- 复用 `RuleEditText` 的字号缩放逻辑，保留原文本区双指缩放能力。

## 1.6.1 - 2026-04-25

- 主界面右上角新增“更多”按钮，点击后展开右侧操作菜单。
- 主界面只保留“保存规则”和“检查”两个常用按钮，其余操作移入“更多”菜单：说明、悬浮窗授权、格式化、复制全部、导出规则、导入旧规则、最近记录、清空记录和诊断日志开关。
- 移除主页 `A-` / `A+` 缩放按钮，减少底部操作区占用。

## 1.6.0 - 2026-04-25

- 修复 ask 悬浮窗 Activity 文本显示和长按复制格式：统一为规则可直接使用的 `包名/.Activity` 或 `包名/完整类名`。
- 移除旧的 `包名 空格 .Activity` 格式，避免复制后不能直接粘贴到规则来源或目标位置。

## 1.5.9 - 2026-04-25

- 补充发布文档：新增 `docs/release-checklist.md` 和 `docs/release-assets.md`，明确发布前检查、APK/源码归档命名和 GitHub Release 附件建议。
- 更新 `README.md`，补充 `XposedJumpAppInterceptor` 的包名、LSPosed 作用域、规则格式、版本规则和常用测试/编译命令。
- 复查第 9 项权限管理器绕过隔离，移除抽离后残留的未使用 `HookEntry.safeText()` 方法；未改变 hook 行为。

## 1.5.8 - 2026-04-25

- 新增 `PermissionBypassController`，集中管理权限管理器绕过链路中的 pending allow、权限弹窗回放、agree 后一次性放行保存和 ask 同意回放恢复。
- `HookEntry` 进一步收敛为规则裁决主流程，ColorOS/OPlus 权限管理器相关处理不再散落在主入口中。
- 本次为结构隔离，不改变现有权限管理器绕过行为。

## 1.5.7 - 2026-04-25

- 新增 `ActivityStarterRequestCompat`，集中封装 `ActivityStarter$Request` 的字段读取、callingUid 兼容读取和请求回写。
- `HookEntry` 主流程不再直接散落 framework 请求字段名，后续适配 Android/ROM 字段变化时优先修改兼容层。
- 本次为结构隔离，不改变 Activity 启动规则裁决和 ask 回放行为。

## 1.5.6 - 2026-04-25

- 新增诊断日志开关，主页按钮可在“诊断日志开/关”之间切换。
- 高频规则命中、ask 决策、权限管理器回放诊断日志改为受开关控制，错误和关键 hook 初始化日志仍保持常开。
- 诊断日志设置通过 provider 缓存到 system_server，并在设置变化时广播刷新，避免每次 Activity 启动都跨进程查询。

## 1.5.5 - 2026-04-25

- 主页新增规则概览区，实时显示有效规则总数、agree/disagree/ask 数量、无效行数量和冲突数量。
- 规则概览区独立于规则文本存储，不会向规则编辑框写入任何自动注释。
- 概览文本支持选中复制，便于后续反馈规则状态。

## 1.5.4 - 2026-04-25

- 启用 `XposedJumpAppInterceptor` 本地 JUnit 测试依赖。
- 新增规则引擎单元测试，覆盖 `allow` 别名、规则顺序、ask 后置匹配、应用内默认同意、显式应用内规则、`system` 别名和冲突检测。
- 后续调整规则解析和裁决逻辑时，可通过 `:XposedJumpAppInterceptor:testDebugUnitTest` 先验证核心行为。

## 1.5.3 - 2026-04-25

- 新增最近拦截记录：规则命中后的 `AGREE`、`DISAGREE`、`ASK`、`ASK_EXEMPT` 会异步写入最近记录，最多保留 50 条。
- 主页新增“最近记录”折叠区和“清空记录”按钮，记录文本支持 Android 原生选中复制。
- 最近记录写入走 provider 后台线程，避免在 `ActivityStarter` hook 链路里同步阻塞 system_server。

## 1.5.2 - 2026-04-25

- 拆分 `HookEntry`：将 ask 豁免包名策略移入 `HookPackagePolicy`，将 system_server 广播和规则刷新桥接移入 `SystemServerBridge`。
- 将权限管理器弹窗回放逻辑移入 `PermissionPromptHook`，保留原有 ColorOS/OPlus 权限管理器接管链路行为。
- 本次为结构拆分，不改变 agree/disagree/ask 裁决顺序和悬浮窗决策流程。

## 1.5.1 - 2026-04-25

- 加固旧模块规则迁移：自动导入只在当前规则为空时写入，避免新包名首次启动时覆盖用户已有规则。
- 手动“导入旧规则”改为合并去重，会保留当前规则和注释，只追加旧模块中尚不存在的有效规则。
- 新增旧规则迁移状态记录，可区分已导入、已合并、已存在、因已有规则跳过和未找到旧规则。

## 1.5.0 - 2026-04-25

- Java 源码包名从 `com.magicianguo.xposedjumpappinterceptor` 迁移到 `t8numen.activitystartmanager`，主源码、测试模板目录、Manifest 组件名、布局自定义 View、Gradle namespace 和 `xposed_init` 入口保持一致。
- 保留 `OLD_MODULE_PACKAGE = "com.magicianguo.xposedjumpappinterceptor"`，继续用于识别旧模块和导入旧规则，避免包名迁移后丢失历史配置。
- 将版本规则调整为三段数字格式，当前大阶段版本推进到 `1.5.0`。

## 1.4.1 - 2026-04-25

- 旧模块安装提示右侧新增关闭按钮，点击后记住状态，不再显示该提示。
- 规则检查文案改为显示“第 N 行”，避免误解为发现 N 条无效规则；同时支持 `allow` 作为 `agree` 的别名，格式化时会统一改为 `agree`。
- 规则配置页软键盘策略改为不调整布局，避免编辑时底部按钮区被键盘顶上来。

## 1.4.0 - 2026-04-25

- 安装包名迁移为 `t8numen.activitystartmanager`，应用名保持 `Activity启动管理`；Java 源码 package 暂不迁移，后续计划统一改名。
- provider authority 迁移为 `t8numen.activitystartmanager.rules`，Manifest 组件改为全限定类名，避免包名变更后组件解析错误。
- 新模块首次打开会尝试从旧 provider `com.magicianguo.xposedjumpappinterceptor.rules` 导入旧规则；也提供手动“导入旧规则”按钮。导入时会过滤旧版本自动生成的 `#` 说明行。
- 规则区默认不再写入 `#` 注释说明；说明区改为可折叠且支持 Android 原生文本选择/复制。
- 规则页新增旧模块安装提示、格式化规则、检查规则、复制全部、导出规则等操作入口。
- 后续计划：Java 源码 package 统一迁移为 `t8numen.activitystartmanager`；新增最近拦截记录和从记录一键生成规则。

## 1.3.18 - 2026-04-24

- 新增应用内跳转内置策略：同一包名内的 Activity 跳转默认同意，不再被 `ask * *` 等兜底规则频繁打断；如需阻止或询问，使用 `disagree com.foo com.foo` 或 `ask com.foo com.foo` 这类同包规则。
- 新增 `system` 规则别名，可使用 `ask system *` 调查系统 UID、系统预装应用或更新系统应用拉起其他应用的行为。
- 更新规则说明，明确应用内跳转默认同意和 `system` 用法。

## 1.3.17 - 2026-04-24

- 调整 `ask` 豁免策略：`ask * *` 仍默认避开桌面来源和少量保命链路；更具体的 `ask A B`、`ask A *`、`ask * B` 可用于调查系统应用、桌面或安装器相关 Activity 调用链。
- 修复规则保存后在 `system_server` 中短时间不生效的问题：保存规则时主动通知系统进程刷新缓存，并注册规则 provider 变化监听作为兜底。
- 规则编辑框外层增加横向滚动容器，编辑区固定为较宽宽度，便于通过横向滚动查看长包名/长 Activity 规则。

## 1.3.16 - 2026-04-24

- 改善 `ask * *` 作为默认询问规则的覆盖能力：当框架请求缺少 `callingPackage` 或 `activityInfo` 时，补充从 `callingUid` 和 `resolveInfo.activityInfo` 解析来源/目标，减少兜底规则被提前跳过的情况。
- 修复 `ask * *` 会询问“桌面打开应用”的问题：桌面包作为来源时也加入 `ask` 豁免，避免从桌面正常打开应用被弹窗打断。
- 长按复制 Activity 信息成功后显示“已复制”提示，并在约 1.2 秒后自动关闭。

## 1.3.15 - 2026-04-24

- 修复 `ask` 点击“同意”后仍无法打开目标 Activity 的问题：同意按钮不再由模块 App 直接 `startActivity`，改为发送一次性决策广播，由 `system_server` 内的 Hook 接收后使用原始启动请求执行重放，绕过 Android 15 的后台启动限制 `BAL_BLOCK`。
- `ask` 拒绝、超时和点击空白处关闭时也会通知 `system_server` 清理对应的一次性请求记录，减少过期请求残留。

## 1.3.14 - 2026-04-24

- 修复 `ask` 点击“同意”后没有打开目标 Activity 的问题：同意重放时携带 requestId，framework 侧恢复原始启动请求、`ActivityInfo`、`ResolveInfo` 和调用包信息。
- `ask` 悬浮窗改为全屏透明触摸层承载底部卡片，点击卡片外空白处会忽略请求并关闭悬浮窗。

## 1.3.13 - 2026-04-24

- `ask` 悬浮窗顶部新增标题“Activity链式启动器”，右侧显示倒计时“X秒后忽略请求”。
- 增加 `QUERY_ALL_PACKAGES` 权限，并优先读取 Activity 图标，改善悬浮窗应用图标和应用名显示。
- 长按复制改为绑定到来源/目标整列和包名 Activity 行，复制完整未省略文本。

## 1.3.12 - 2026-04-24

- `ask` 弹窗从 SystemUI 附着方案切换为模块自身 `SYSTEM_ALERT_WINDOW` 系统悬浮窗方案，降低 ROM/作用域导致的不弹窗概率。
- 新增模块广播接收器和 `Application` 初始化，`system_server` 命中 `ask` 后会唤起模块进程显示底部悬浮窗。
- 配置页新增悬浮窗权限入口，便于直接跳转到系统授权页。
- `system_server` 启动 hook 后会后台预热规则缓存，减少重启后首次启动未命中规则的问题。
- 规则编辑框支持双指缩放和 `A-` / `A+` 按钮缩放，长规则可横向滚动查看。
- 配置页顶部新增应用名和右下对齐的小号版本号。

## 1.3.11 - 2026-04-24

- 调整规则裁决顺序：先按规则顺序匹配 `agree/disagree`，两者都未命中时再按规则顺序匹配 `ask`。
- `ask` 与 `agree/disagree` 重叠不再作为冲突提示，便于使用 `ask * *` 作为兜底规则。
- 保留 `agree/disagree` 之间的冲突检测和高亮提示。

## 1.3.10 - 2026-04-24

- 补强 `ask` 弹窗触发：SystemUI 侧新增 `ContentObserver` 监听待询问请求变化，并在初始化时主动查询一次，降低广播未送达导致不弹窗的概率。
- 规则编辑框新增行号显示，空行不显示行号；编辑框改为横向滚动，避免长包名自动换行导致行号错位。
- 冲突保存提示改为短 Toast，并对相关冲突行高亮 1 秒，便于快速定位。

## 1.3.9 - 2026-04-24

- 初步实现 `ask`：命中规则后拦截原始启动，并在 `SystemUI` 进程显示屏幕底部询问弹窗。
- `ask` 弹窗显示来源/目标应用图标、应用名、包名和 Activity，按钮为“同意”“拒绝”，结果仅对本次启动生效。
- `ask` 超时 15 秒自动拒绝；同时出现多个请求时只保留最新请求并关闭旧弹窗。
- 同意后复用内部重放与 `pending_allow` 放行链路，尽量避免 ColorOS / OPlus 权限管理器再次弹窗。
- 新增 `com.android.systemui` 到 LSPosed 作用域；`ask * *` 对模块自身、SystemUI、权限管理器、包安装器和桌面目标等保命路径放行。

## 1.3.8 - 2026-04-24

- 规则执行顺序改为从上到下匹配，命中第一条后停止，不再按具体度或后写规则覆盖。
- 保存规则时检测与前面规则重叠且动作不同的冲突，使用 Toast 提示冲突行号。
- 移除配置页的“加载示例”按钮，避免误触覆盖已有规则。

## 1.3.7 - 2026-04-24

- 修复 `system_server` watchdog 重启问题：`ActivityStarter.executeRequest` hook 内不再同步访问模块 `ContentProvider`。
- 规则读取改为内存缓存返回、后台线程刷新，避免在 `WindowManagerGlobalLock` 持锁期间等待 `ActivityManagerService`。
- `pending_allow` 的保存与清理改为后台执行，避免 framework 启动链路内出现同类锁反转。
- 规则缓存刷新间隔调整为 5 秒，降低 system_server 内跨进程读取频率。

## 1.3.6 - 2026-04-24

- 增强权限管理器链路诊断日志，统一使用 `diag.pm.*` 前缀，明确区分 `pending_saved`、`detected`、`framework_bypass`、`ui_appeared`、`ui_replayed` 等阶段。
- 保持当前 `agree` 的放行行为不变，仅补充更适合定位 `com.oplus.securitypermission` / `com.android.permissioncontroller` 的排查信息。

## 1.3.5 - 2026-04-24

- `agree` 新增跨进程 `pending_allow` 状态，通过 `ContentProvider` 在 `android` 与权限管理器进程之间共享一次性放行信息。
- 模块 scope 扩展为 `android`、`com.oplus.securitypermission`、`com.coloros.securitypermission`、`com.android.permissioncontroller`，开始尝试在权限管理器进程内直接回放原始目标 Activity。
- 日志前缀改为 ASCII 的 `[ALC]`，便于从 LSPosed 导出后继续排查，不再依赖中文标题前缀。
- APK 归档改为同时输出当前包名和带版本号的包名，方便后续按版本管理。

## 1.3.4 - 2026-04-24

- 修正规则页布局，避免顶部说明与编辑框内容被标题栏挤压或遮挡。
- 为 `agree` 增加更详细的权限管理器接管诊断日志，便于继续定位 ColorOS / OPlus 的启动弹窗链路。
- 继续保持 `system framework` 侧的 `agree/disagree` 规则裁决。

## 1.3.2 - 2026-04-24

- 将 `XposedJumpAppInterceptor` 改为优先工作在 `system framework`，按规则处理 Activity 启动。
- 新增规则配置页、规则解析与 `ContentProvider` 读取链路，便于模块与 framework 进程共享规则。
- 为 `agree` 增加短时放行上下文，尝试绕过 ColorOS / OPlus 权限管理器的二次启动弹窗。
- 将模块内主要可见文案汉化。
- 建立版本记录文件，后续按版本维护变更。
