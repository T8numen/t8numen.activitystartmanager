# Xposed Modules Repo 提交说明

官方要求以 Xposed Modules Repo 当前说明为准：

- https://github.com/Xposed-Modules-Repo
- https://github.com/Xposed-Modules-Repo/submission
- https://modules.lsposed.org/submission/

## 本模块提交信息

- 模块名：`Activity启动管理`
- 包名：`t8numen.activitystartmanager`
- GitHub 仓库名：`t8numen.activitystartmanager`
- GitHub 仓库描述：`Activity启动管理`
- 当前候选版本：`1.7.1`
- 当前候选 versionCode：`42`
- Release tag：`42-1.7.1`

## 首次发布步骤

1. 在 GitHub 创建公开仓库 `t8numen.activitystartmanager`。
2. 确认仓库描述为 `Activity启动管理`。
3. 推送源码，仓库根目录保留 `README.md` 和 `SUMMARY`。
4. 创建 GitHub Release，tag 使用 `42-1.7.1`。
5. 上传 release APK、SHA256 校验文件和源码归档。
6. 通过 https://modules.lsposed.org/submission/ 提交包名。

## Release 文案模板

```text
Activity启动管理 1.7.1

测试环境：
- LSPosed 1.9.2
- ColorOS / Android 15

主要变化：
- 撤回 1.7.0 中 system_server 同步查询规则 provider 的实现，避免开机后首次打开应用时可能卡住 system_server。
- Activity 启动链路和规则更新广播不再等待 provider 查询完成，所有规则刷新都回到后台线程执行。
- provider 查询失败时不再把失败时间当作有效缓存时间，后续启动会继续请求后台刷新。

安装/更新：
- 安装后在 LSPosed 中启用模块。
- 涉及 system_server hook，建议重启手机。

风险提示：
- 错误规则可能影响应用启动。
- 如误配导致异常，可禁用模块并重启后再修改规则。
```

## 注意事项

- 不要上传 debug APK。
- 不要提交 `release-signing.properties` 或 `local-signing/`。
- 后续每个版本的 Release tag 都应使用 `VERSION_CODE-VERSION_NAME`。
