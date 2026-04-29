# 发布产物规则

本项目按版本保留 APK、校验值和源码归档，便于回退、对比和 GitHub Release 发布。

## 目录

- `apks/`：保存本地构建出的 APK，已加入 `.gitignore`。
- `source-archives/`：保存对应版本源码压缩包。
- `release-artifacts/`：保存准备上传 GitHub Release 的发布资产，已加入 `.gitignore`。
- `history.md`：保存每个版本的变更记录。
- `docs/release-checklist.md`：保存发布前检查步骤。

## APK 命名

调试 APK：

```text
apks/activitystartmanager-debug.apk
apks/activitystartmanager-debug-v版本号.apk
```

发布 APK：

```text
apks/activitystartmanager-release.apk
apks/activitystartmanager-release-v版本号.apk
```

上传 Xposed Modules Repo / GitHub Release 时应使用 release APK，不应使用 debug APK。

## 源码归档命名

```text
source-archives/t8numen.activitystartmanager-v版本号.zip
```

源码归档必须排除：

- `.git/`
- `.gradle/`
- `.gradle-local/`
- `build/`
- `apks/`
- `release-artifacts/`
- `source-archives/`
- `local.properties`
- `release-signing.properties`
- `local-signing/`

## 校验值

每个发布版本生成：

```text
release-artifacts/SHA256SUMS-v版本号.txt
```

内容至少包含：

- release APK 的 SHA256
- 源码归档 zip 的 SHA256

## GitHub Release 附件

首次发布建议上传：

1. `activitystartmanager-release-v版本号.apk`
2. `SHA256SUMS-v版本号.txt`
3. `t8numen.activitystartmanager-v版本号.zip`

Release tag 必须使用：

```text
VERSION_CODE-VERSION_NAME
```

例如：

```text
41-1.7.0
```

## 当前维护约定

- 版本格式为 `a.b.c`。
- 每段使用一个数字位，满 10 进一。
- 小修补提升 `c`，结构或行为大改可提升 `b`。
- 每次改动默认更新 `history.md`、编译 APK、归档源码和 APK。
