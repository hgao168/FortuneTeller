const history = require('../../utils/history.js');

const SCOPE_LABELS = {
  today: '今日',
  month: '本月',
  year: '本年',
  long_term: '长期运势'
};

function formatItem(item) {
  let title = '';
  let subtitle = '';
  if (item.type === 'palm') {
    title = (SCOPE_LABELS[item.scope] || item.scope) + ' · 手相解读';
    subtitle = (item.data && item.data.summary) || '';
  } else if (item.type === 'match') {
    const label = item.matchType === 'romantic' ? '男女配对' : '朋友配对';
    title = label + (item.data && item.data.score != null ? ` · ${item.data.score} 分` : '');
    subtitle = (item.data && item.data.summary) || '';
  }
  const d = new Date(item.date);
  const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  return { id: item.id, type: item.type, title, subtitle, dateStr, raw: item };
}

Page({
  data: {
    list: []
  },

  onShow() {
    const raw = history.loadAll();
    this.setData({ list: raw.map(formatItem) });
  },

  openItem(e) {
    const id = e.currentTarget.dataset.id;
    const entry = this.data.list.find((x) => x.id === id);
    if (!entry) return;
    if (entry.type === 'palm') {
      wx.setStorageSync('ft_last_palm', entry.raw.data);
      wx.navigateTo({ url: '/pages/result/index' });
    } else {
      wx.setStorageSync('ft_last_match', entry.raw.data);
      wx.navigateTo({ url: '/pages/match-result/index' });
    }
  },

  clearAll() {
    wx.showModal({
      title: '清空历史记录？',
      content: '此操作不可撤销',
      success: (res) => {
        if (res.confirm) {
          history.clear();
          this.setData({ list: [] });
        }
      }
    });
  }
});
