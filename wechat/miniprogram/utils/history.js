// 本地历史记录存储
const app = getApp();

function getKey() {
  return app.globalData.historyKey;
}

function loadAll() {
  try {
    return wx.getStorageSync(getKey()) || [];
  } catch (e) {
    return [];
  }
}

function saveAll(list) {
  try {
    wx.setStorageSync(getKey(), list);
  } catch (e) {}
}

function add(entry) {
  const list = loadAll();
  list.unshift(Object.assign({ id: Date.now().toString(), date: Date.now() }, entry));
  // 最多保留 50 条
  if (list.length > 50) list.length = 50;
  saveAll(list);
}

function remove(id) {
  const list = loadAll().filter((x) => x.id !== id);
  saveAll(list);
}

function clear() {
  saveAll([]);
}

module.exports = { loadAll, add, remove, clear };
