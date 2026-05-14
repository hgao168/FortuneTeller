const SCOPE_LABELS = {
  today: '今日',
  month: '本月',
  year: '本年',
  long_term: '长期运势'
};

Page({
  data: {
    result: null,
    scopeLabel: ''
  },

  onLoad() {
    const result = wx.getStorageSync('ft_last_palm');
    if (!result) {
      wx.showToast({ title: '没有可显示的结果', icon: 'none' });
      return;
    }
    this.setData({
      result,
      scopeLabel: SCOPE_LABELS[result.scope] || result.scope
    });
  },

  goHome() {
    wx.switchTab({ url: '/pages/read/index' });
  }
});
