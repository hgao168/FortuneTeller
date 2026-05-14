// 小程序入口
App({
  globalData: {
    // 后端 API，部署在 Railway（与 iOS 客户端一致）
    baseURL: 'https://fortuneteller-production-f93e.up.railway.app',
    // 默认中文，符合微信生态用户习惯
    language: 'zh-Hans',
    // 历史记录存储键
    historyKey: 'ft_history_v1'
  },

  onLaunch() {
    // 预热网络
    try {
      wx.request({
        url: this.globalData.baseURL + '/health',
        method: 'GET',
        timeout: 5000,
        success: () => {},
        fail: () => {}
      });
    } catch (e) {}
  }
});
