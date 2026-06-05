/**
 * Prism.js language definition for JoshLang
 * 
 * @license BSD-3-Clause
 */

(function(Prism) {
  Prism.languages.joshlang = {
    'comment': {
      pattern: /#.*/,
      greedy: true
    },
    'string': {
      pattern: /"[^"]*"/,
      greedy: true
    },
    'keyword': /\b(?:start|end|simulation|patch|organism|disturbance|external|management|unit|state|agent|alias|all|and|as|assert|at|config|const|create|current|elif|else|export|exportFiles|false|force|from|grid|here|if|import|init|latitude|limit|longitude|map|mean|normal|of|or|prior|radial|replacement|return|sample|std|step|steps|to|true|uniform|with|within|without|xor)\b/,
    'function': /\b(?:mean|std|sample|create|limit|map)\b/,
    'boolean': /\b(?:true|false)\b/,
    'number': [
      {
        pattern: /(?:\b\d+(?:\.\d+)?|\B\.\d+)(?:[eE][+-]?\d+)?/,
        greedy: true
      },
      {
        pattern: /\d+(?:\.\d+)?%/,
        greedy: true
      }
    ],
    'operator': /\+|-|\*|\/|\^|==|!=|<=|>=|<|>|=|\||#/,
    'punctuation': /[{}[\](),.;:]/,
    'property': {
      pattern: /\b[a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)+/,
      inside: {
        'punctuation': /\./
      }
    },
    'variable': /\b[a-zA-Z_]\w*\b/
  };

  // Set up autoloader path for JoshLang if needed
  if (Prism.plugins.autoloader) {
    Prism.plugins.autoloader.languages_path = '/';
  }

})(Prism);