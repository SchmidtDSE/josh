/**
 * Ace Editor mode for JoshLang
 * 
 * @license BSD-3-Clause
 */

ace.define("ace/mode/joshlang_highlight_rules",["require","exports","module","ace/lib/oop","ace/mode/text_highlight_rules"], function(require, exports, module) {
  "use strict";
  
  var oop = require("../lib/oop");
  var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;
  
  var JoshLangHighlightRules = function() {
    
    var keywords = (
      "start|end|simulation|patch|organism|disturbance|external|management|" +
      "unit|state|agent|alias|all|and|as|assert|at|config|const|create|" +
      "current|elif|else|export|exportFiles|false|force|from|grid|here|if|import|init|latitude|" +
      "limit|longitude|map|mean|normal|of|or|prior|radial|replacement|" +
      "return|sample|std|step|steps|to|true|uniform|with|within|without|xor"
    );
    
    var builtinFunctions = (
      "mean|std|sample|create|limit|map"
    );
    
    var builtinConstants = (
      "true|false"
    );
    
    var keywordMapper = this.createKeywordMapper({
      "keyword": keywords,
      "support.function": builtinFunctions,
      "constant.language": builtinConstants
    }, "identifier");
    
    this.$rules = {
      "start": [
        {
          token: "comment",
          regex: "#.*$"
        },
        {
          token: "string",
          regex: '"[^"]*"'
        },
        {
          token: "constant.numeric",
          regex: "\\d+(?:\\.\\d+)?%"
        },
        {
          token: "constant.numeric",
          regex: "-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?"
        },
        {
          token: "keyword.operator",
          regex: "\\+|-|\\*|\\/|\\^|==|!=|<=|>=|<|>|=|\\||#"
        },
        {
          token: "paren.lparen",
          regex: "[\\[\\(\\{]"
        },
        {
          token: "paren.rparen",
          regex: "[\\]\\)\\}]"
        },
        {
          token: "punctuation.operator",
          regex: "[,;:]"
        },
        {
          token: "variable.language",
          regex: "\\b(?:meta|prior|current|here)\\b"
        },
        {
          token: keywordMapper,
          regex: "\\b[a-zA-Z_]\\w*\\b"
        },
        {
          token: "text",
          regex: "\\s+"
        }
      ]
    };
    
    this.normalizeRules();
  };
  
  oop.inherits(JoshLangHighlightRules, TextHighlightRules);
  
  exports.JoshLangHighlightRules = JoshLangHighlightRules;
});

ace.define("ace/mode/joshlang",["require","exports","module","ace/lib/oop","ace/mode/text","ace/mode/joshlang_highlight_rules"], function(require, exports, module) {
  "use strict";
  
  var oop = require("../lib/oop");
  var TextMode = require("./text").Mode;
  var JoshLangHighlightRules = require("./joshlang_highlight_rules").JoshLangHighlightRules;
  
  var Mode = function() {
    this.HighlightRules = JoshLangHighlightRules;
    this.$behaviour = this.$defaultBehaviour;
  };
  oop.inherits(Mode, TextMode);
  
  (function() {
    this.lineCommentStart = "#";
    this.$id = "ace/mode/joshlang";
  }).call(Mode.prototype);
  
  exports.Mode = Mode;
});