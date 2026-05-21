// 小程序入口
App({
  globalData: {
    // TODO: Inject BACKEND_BASE_URL at build/CI time (env var or build variable placeholder)
    baseURL: process.env.BACKEND_BASE_URL || 'https://fortuneteller-production-f93e.up.railway.app',
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
