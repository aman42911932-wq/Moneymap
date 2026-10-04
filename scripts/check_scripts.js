const fs = require('fs');
const vm = require('vm');
const path = require('path');

const webappDir = path.join(__dirname, '..', 'src', 'main', 'webapp');
const htmlFiles = fs.readdirSync(webappDir).filter(f => f.endsWith('.html'));

let hasError = false;
htmlFiles.forEach(file => {
    const fullPath = path.join(webappDir, file);
    const content = fs.readFileSync(fullPath, 'utf8');
    const scriptMatches = content.match(/<script(?![^>]*src=)[^>]*>([\s\S]*?)<\/script>/gi) || [];
    scriptMatches.forEach((scriptTag, idx) => {
        const code = scriptTag.replace(/<script[^>]*>|<\/script>/gi, '');
        try {
            new vm.Script(code, { filename: file + '#script' + idx });
            console.log('PASS: ' + file + ' (inline script ' + idx + ')');
        } catch (e) {
            console.error('FAIL: ' + file + ' (inline script ' + idx + '): ' + e.message);
            hasError = true;
        }
    });
});

['js/api.js', 'js/app.js'].forEach(jsFile => {
    const fullPath = path.join(webappDir, jsFile);
    const code = fs.readFileSync(fullPath, 'utf8');
    try {
        new vm.Script(code, { filename: jsFile });
        console.log('PASS: ' + jsFile);
    } catch (e) {
        console.error('FAIL: ' + jsFile + ': ' + e.message);
        hasError = true;
    }
});

if (hasError) process.exit(1);
console.log('All frontend scripts passed node syntax check!');
