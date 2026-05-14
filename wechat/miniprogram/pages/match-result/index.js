Page({
  data: {
    result: null,
    typeLabel: ''
  },

  onLoad() {
    const result = wx.getStorageSync('ft_last_match');
    if (!result) {
      wx.showToast({ title: '没有可显示的结果', icon: 'none' });
      return;
    }
    const typeLabel = result.match_type === 'romantic' ? '男女配对' : '朋友配对';
    this.setData({ result, typeLabel });
  },

  goHome() {
    wx.switchTab({ url: '/pages/match/index' });
  }
});
