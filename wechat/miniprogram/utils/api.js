// 封装后端 API 调用（与 iOS APIClient 对齐）
const app = getApp();

function baseURL() {
  return app.globalData.baseURL;
}

function language() {
  return app.globalData.language || 'zh-Hans';
}

/**
 * 上传单张手掌图，调用 /analyze-palm
 * @param {Object} opts
 * @param {string} opts.filePath 本地图片临时路径
 * @param {string} opts.scope today | month | year | long_term
 */
function analyzePalm({ filePath, scope }) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: baseURL() + '/analyze-palm',
      filePath,
      name: 'image',
      formData: {
        scope,
        language: language()
      },
      timeout: 60000,
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            resolve(JSON.parse(res.data));
          } catch (e) {
            reject(new Error('返回数据解析失败'));
          }
        } else {
          reject(new Error(parseError(res.data, res.statusCode)));
        }
      },
      fail(err) {
        reject(new Error(err.errMsg || '网络请求失败'));
      }
    });
  });
}

/**
 * 配对解读：因 wx.uploadFile 一次只能传一个文件，
 * 这里使用 wx.request + base64，绕过限制。
 */
function matchPalm({ filePathA, filePathB, matchType, personABirth, personBBirth }) {
  return new Promise((resolve, reject) => {
    // 微信小程序的 wx.uploadFile 只支持一个文件字段，
    // 因此把第二张图片作为 multipart 字段附加：使用底层 task。
    // 简化方案：先上传 A，并将 B 以 base64 form 字段一并提交。
    // 为兼容后端 multipart，使用 wx.getFileSystemManager 读取并自行拼装 multipart。
    const fs = wx.getFileSystemManager();
    let dataA, dataB;
    try {
      dataA = fs.readFileSync(filePathA);
      dataB = fs.readFileSync(filePathB);
    } catch (e) {
      reject(new Error('读取图片失败'));
      return;
    }

    const boundary = '----WeChatFormBoundary' + Date.now();
    const body = buildMultipart(boundary, [
      { name: 'match_type', value: matchType },
      { name: 'language', value: language() },
      { name: 'person_a_birth', value: personABirth },
      { name: 'person_b_birth', value: personBBirth }
    ], [
      { name: 'image_a', filename: 'palm-a.jpg', contentType: 'image/jpeg', data: dataA },
      { name: 'image_b', filename: 'palm-b.jpg', contentType: 'image/jpeg', data: dataB }
    ]);

    wx.request({
      url: baseURL() + '/match-palm',
      method: 'POST',
      header: {
        'content-type': 'multipart/form-data; boundary=' + boundary
      },
      data: body,
      timeout: 60000,
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else {
          reject(new Error(parseError(res.data, res.statusCode)));
        }
      },
      fail(err) {
        reject(new Error(err.errMsg || '网络请求失败'));
      }
    });
  });
}

function buildMultipart(boundary, fields, files) {
  // 返回 ArrayBuffer
  const parts = [];
  const enc = (str) => {
    // UTF-8 encode -> Uint8Array
    const utf8 = unescape(encodeURIComponent(str));
    const buf = new Uint8Array(utf8.length);
    for (let i = 0; i < utf8.length; i++) buf[i] = utf8.charCodeAt(i) & 0xff;
    return buf;
  };

  fields.forEach((f) => {
    const head = `--${boundary}\r\nContent-Disposition: form-data; name="${f.name}"\r\n\r\n`;
    parts.push(enc(head));
    parts.push(enc(String(f.value)));
    parts.push(enc('\r\n'));
  });

  files.forEach((f) => {
    const head = `--${boundary}\r\nContent-Disposition: form-data; name="${f.name}"; filename="${f.filename}"\r\nContent-Type: ${f.contentType}\r\n\r\n`;
    parts.push(enc(head));
    parts.push(new Uint8Array(f.data));
    parts.push(enc('\r\n'));
  });

  parts.push(enc(`--${boundary}--\r\n`));

  let total = 0;
  parts.forEach((p) => (total += p.length));
  const out = new Uint8Array(total);
  let offset = 0;
  parts.forEach((p) => {
    out.set(p, offset);
    offset += p.length;
  });
  return out.buffer;
}

function parseError(data, code) {
  try {
    const obj = typeof data === 'string' ? JSON.parse(data) : data;
    if (obj && obj.detail) return String(obj.detail);
  } catch (e) {}
  return `服务器返回 ${code}`;
}

module.exports = {
  analyzePalm,
  matchPalm
};
