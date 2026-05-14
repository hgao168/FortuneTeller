const api = require('../../utils/api.js');
const history = require('../../utils/history.js');

const SCOPES = [
  { key: 'today', label: '今日' },
  { key: 'month', label: '本月' },
  { key: 'year', label: '本年' },
  { key: 'long_term', label: '长期运势' }
];

Page({
  data: {
    scopes: SCOPES,
    activeScope: 'today',
    imagePath: '',
    loading: false
  },

  onScopeTap(e) {
    this.setData({ activeScope: e.currentTarget.dataset.key });
  },

  chooseFromCamera() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera'],
      camera: 'back',
      success: (res) => {
        if (res.tempFiles && res.tempFiles[0]) {
          this.setData({ imagePath: res.tempFiles[0].tempFilePath });
        }
      }
    });
  },

  chooseFromAlbum() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album'],
      success: (res) => {
        if (res.tempFiles && res.tempFiles[0]) {
          this.setData({ imagePath: res.tempFiles[0].tempFilePath });
        }
      }
    });
  },

  async analyze() {
    if (!this.data.imagePath) {
      wx.showToast({ title: '请先选择手掌照片', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    wx.showLoading({ title: '解读中...', mask: true });
    try {
      const result = await api.analyzePalm({
        filePath: this.data.imagePath,
        scope: this.data.activeScope
      });
      // 写入历史
      history.add({ type: 'palm', scope: this.data.activeScope, data: result });
      // 缓存当前结果传给结果页
      wx.setStorageSync('ft_last_palm', result);
      wx.navigateTo({ url: '/pages/result/index' });
    } catch (e) {
      wx.showModal({ title: '解读失败', content: e.message || '请稍后再试', showCancel: false });
    } finally {
      wx.hideLoading();
      this.setData({ loading: false });
    }
  }
});
