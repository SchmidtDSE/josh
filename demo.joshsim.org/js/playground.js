/**
 * PlaygroundPresenter: manages the interactive Ace editor playground for demo.joshsim.org.
 *
 * Owns an Ace editor showing the full ForeverTree model (joshlang mode, textmate theme) and a
 * collapsible configuration textarea. Ace is lazy-initialized on the first onShow() call so that
 * it never tries to size itself while the containing section is display:none.
 *
 * Components 6 and 7 extend this class by enabling the Run button and wiring the results panel.
 *
 * @license BSD-3-Clause
 */


class PlaygroundPresenter {

  /**
   * @param {string} editorId         - id of the <div> Ace will own.
   * @param {string} configTextareaId - id of the <textarea> for the .jshc config.
   * @param {string[]} codeLines      - Full model code as an array of lines (from FOREVERTREE_WASM_SNAPSHOT).
   * @param {string} configPath       - URL/path to fetch the .jshc config file at runtime.
   */
  constructor(editorId, configTextareaId, codeLines, configPath) {
    this._editorId = editorId;
    this._configTextareaId = configTextareaId;
    this._codeLines = codeLines;
    this._configPath = configPath;
    this._aceInitialized = false;
    this._editor = null;
    this._configTextarea = null;
  }

  /**
   * Called by NarrativePresenter after the playground section becomes visible (post-fade-in).
   *
   * Lazy-initializes the Ace editor the first time, then just resizes on subsequent shows.
   * Also fetches the config file and populates the config textarea (first call only).
   */
  onShow() {
    const self = this;

    if (self._aceInitialized) {
      if (self._editor) {
        self._editor.resize(true);
      }
      return;
    }

    self._aceInitialized = true;

    self._configTextarea = document.getElementById(self._configTextareaId);

    ace.config.set("basePath", "./third_party");

    self._editor = ace.edit(self._editorId);
    self._editor.getSession().setUseWorker(false);
    self._editor.setTheme("ace/theme/textmate");
    self._editor.getSession().setMode("ace/mode/joshlang");
    self._editor.session.setOptions({ tabSize: 2, useSoftTabs: true });
    self._editor.getSession().setValue(self._codeLines.join("\n"), 1);

    setTimeout(() => {
      if (self._editor) {
        self._editor.resize(true);
      }
    }, 50);

    fetch(self._configPath)
      .then(function(r) {
        if (!r.ok) {
          throw new Error("HTTP " + r.status);
        }
        return r.text();
      })
      .then(function(text) {
        if (self._configTextarea) {
          self._configTextarea.value = text;
        }
      })
      .catch(function(err) {
        console.warn("PlaygroundPresenter: could not load config from " + self._configPath + ":", err);
      });
  }

  /**
   * Returns the current Ace editor content as a string.
   *
   * @returns {string} Current model code.
   */
  getCode() {
    if (this._editor) {
      return this._editor.getValue();
    }
    return this._codeLines.join("\n");
  }

  /**
   * Returns the current config textarea content as a string.
   *
   * @returns {string} Current config text.
   */
  getConfig() {
    if (this._configTextarea) {
      return this._configTextarea.value;
    }
    return "";
  }

}


export {PlaygroundPresenter};
