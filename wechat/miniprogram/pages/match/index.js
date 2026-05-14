const api = require('../../utils/api.js');
const history = require('../../utils/history.js');

function formatBirth(dateStr, timeStr) {
  // 后端期望 ISO 8601。本地选择得到 YYYY-MM-DD + HH:MM
  // 拼装为 YYYY-MM-DDTHH:MM:00Z（按本地时区近似）
  if (!dateStr) return '';
  const t = timeStr || '12:00';
  return `${dateStr}T${t}:00Z`;
}

Page({
  data: {
    matchType: 'romantic',
    // 帅哥 / 美女（与 iOS 的 zh-Hans 文案保持一致）
    personA: { label: '帅哥', image: '', birthDate: '', birthTime: '12:00' },
    personB: { label: '美女', image: '', birthDate: '', birthTime: '12:00' },
    loading: false
  },

  setRomantic() { this.setData({ matchType: 'romantic' }); },
  setFriend()   { this.setData({ matchType: 'friend' }); },

  pickImage(e) {
    const who = e.currentTarget.dataset.who;
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        if (res.tempFiles && res.tempFiles[0]) {
          this.setData({ [`${who}.image`]: res.tempFiles[0].tempFilePath });
        }
      }
    });
  },

  onDateChange(e) {
    const who = e.currentTarget.dataset.who;
    this.setData({ [`${who}.birthDate`]: e.detail.value });
  },

  onTimeChange(e) {
    const who = e.currentTarget.dataset.who;
    this.setData({ [`${who}.birthTime`]: e.detail.value });
  },

  async submit() {
    const { personA, personB, matchType } = this.data;
    if (!personA.image || !personB.image) {
      wx.showToast({ title: '请上传两张手掌照片', icon: 'none' });
      return;
    }
    if (!personA.birthDate || !personB.birthDate) {
      wx.showToast({ title: '请填写出生日期', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    wx.showLoading({ title: '配对中...', mask: true });
    try {
      const result = await api.matchPalm({
        filePathA: personA.image,
        filePathB: personB.image,
        matchType,
        personABirth: formatBirth(personA.birthDate, personA.birthTime),
        personBBirth: formatBirth(personB.birthDate, personB.birthTime)
      });
      history.add({ type: 'match', matchType, data: result });
      wx.setStorageSync('ft_last_match', result);
      wx.navigateTo({ url: '/pages/match-result/index' });
    } catch (e) {
      wx.showModal({ title: '配对失败', content: e.message || '请稍后再试', showCancel: false });
    } finally {
      wx.hideLoading();
      this.setData({ loading: false });
    }
  }
});
