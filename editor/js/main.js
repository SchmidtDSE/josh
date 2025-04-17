import {EditorPresenter} from "editor";
import {FilePresenter} from "file";


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


function showContents() {
  document.getElementById("loading").style.display = "none";
  document.getElementById("main-holder").style.display = "block";
}


export {main};
