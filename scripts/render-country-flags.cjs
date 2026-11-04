// Build-time only: npm install --prefix <tool-dir> flag-icons@7.3.2 @resvg/resvg-js@2.6.2
// NODE_PATH=<tool-dir>/node_modules node --expose-gc scripts/render-country-flags.cjs <output-png-dir>
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Resvg } = require('@resvg/resvg-js');
const source = path.join(path.dirname(require.resolve('flag-icons/package.json')), 'flags', '4x3');
if (!process.argv[2]) throw Error('Supply a temporary PNG output directory');
const output = path.resolve(process.argv[2]);
fs.mkdirSync(output, { recursive: true });
const manifest = [];
for (const name of fs.readdirSync(source).filter(name => /^[a-z]{2}\.svg$/.test(name)).sort()) {
    const png = new Resvg(fs.readFileSync(path.join(source, name)), { fitTo: { mode: 'width', value: 2048 } }).render().asPng();
    fs.writeFileSync(path.join(output, name.replace('.svg', '.png')), png);
    manifest.push({ code: name.slice(0, 2).toUpperCase(), width: 2048, height: 1536, sha256: crypto.createHash('sha256').update(png).digest('hex') });
    if (global.gc) global.gc();
}
fs.writeFileSync(path.join(output, 'manifest.json'), JSON.stringify(manifest, null, 2));
