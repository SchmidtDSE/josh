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
    // meta/prior/current/here are intrinsic references; the Ace joshlang mode
    // (editor/js/mode-joshlang.js) tags them as variable.language. Match that here,
    // before 'keyword', so highlighting is consistent with the IDE editor.
    'variable-language': /\b(?:meta|prior|current|here)\b/,
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
    // No 'property' token: the Ace joshlang mode highlights dotted names
    // segment-by-segment (e.g. age.step -> 'age' identifier, 'step' keyword), so we
    // let each segment fall through to keyword/variable and color '.' as punctuation.
    'punctuation': /[{}[\](),.;:]/,
    'variable': /\b[a-zA-Z_]\w*\b/
  };

  // Set up autoloader path for JoshLang if needed
  if (Prism.plugins.autoloader) {
    Prism.plugins.autoloader.languages_path = '/';
  }

})(Prism);