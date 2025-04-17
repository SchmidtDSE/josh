
/**
 * Main entry point for the Josh web editor application.
 * 
 * @license BSD-3-Clause
 */

import {EditorPresenter} from "editor";
import {FilePresenter} from "file";


/**
 * Initializes the editor and file handling components.
 * 
 * Creates and configures the file and editor presenters, sets up event handling,
 * and loads any previously saved code.
 */
function main() {
  const filePresenter = new FilePresenter("file-buttons", (code) => {
    editorPresenter.setCode(code);
  });
  
  const editorPresenter = new EditorPresenter("code-editor", (code) => {
    filePresenter.saveCodeToFile(code);
  });

  const priorCode = filePresenter.getCodeInFile();
  if (priorCode) {
    editorPresenter.setCode(priorCode);
  }

  showContents();
}


/**
 * Shows the main editor interface by hiding the loading screen.
 * 
 * Hides the loading indicator and displays the main editor interface
 * once initialization is complete.
 */
function showContents() {
  document.getElementById("loading").style.display = "none";
  document.getElementById("main-holder").style.display = "block";
}


export {main};
