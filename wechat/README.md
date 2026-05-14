# FortuneTeller 微信小程序

基于 iOS 端 FortuneTeller 移植的微信小程序版本，默认语言为简体中文，复用同一后端服务。

## 功能

- **手相解读**：拍照或从相册选择手掌照片，按 今日 / 本月 / 本年 / 长期 范围获取 AI 解读。
- **手相配对**：上传两人手掌照片 + 出生信息，支持「男女配对（帅哥 × 美女）」和「朋友配对」两种类型。
- **历史记录**：本地存储最多 50 条历史结果，可点击回看或清空。

## 后端

复用已部署的 Railway 服务：

```
https://fortuneteller-production-f93e.up.railway.app
```

接口：
- `POST /analyze-palm` — 字段：`image` (file)、`scope`、`language`
- `POST /match-palm` — 字段：`image_a`、`image_b` (file)、`match_type`、`language`、`person_a_birth`、`person_b_birth`

`language` 字段默认传 `zh-Hans`，由后端返回简体中文结果。

## 目录

```
wechat/
├── project.config.json          # 微信开发者工具项目配置
├── project.private.config.json  # 本地私有配置
└── miniprogram/
    ├── app.js / app.json / app.wxss
    ├── sitemap.json
    ├── utils/
    │   ├── api.js               # 后端调用封装（multipart 双文件上传）
    │   └── history.js           # 本地历史记录
    └── pages/
        ├── read/                # 手相解读
        ├── result/              # 解读结果
        ├── match/               # 配对入口
        ├── match-result/        # 配对结果
        └── history/             # 历史
```

## 本地运行

1. 安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)。
2. 选择「导入项目」，目录指向 `wechat/`。
3. AppID 可使用测试号（`touristappid`），上线前替换为正式 AppID。
4. 由于使用 `wx.request` / `wx.uploadFile` 调用 Railway 域名，发布前需在「微信公众平台 → 开发设置 → 服务器域名」中将 `https://fortuneteller-production-f93e.up.railway.app` 加入 `request` 与 `uploadFile` 合法域名。开发期可在开发者工具的「详情 → 本地设置」中勾选「不校验合法域名」。

## 设计风格

与 iOS 端深色趣味主题保持一致：深紫渐变背景、玻璃卡片、橙→粉→蓝渐变按钮，配对类型采用「粉色 = 男女」「蓝色 = 朋友」色彩语义。
