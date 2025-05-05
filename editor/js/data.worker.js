/**
 * Worker thread to manage OPFS.
 *
 * @license BSD-3-Clause
 * @author Sketchingpy Project
 */

/**
 * A decorator class that manages file operations through a worker thread interface.
 */
class ReceiverFileManagerDecorator {
    /**
     * Creates a new ReceiverFileManagerDecorator.
     * @param {Object} inner - The inner file manager implementation.
     */
    constructor(inner) {
        const self = this;
        self._inner = inner;
    }

    /**
     * Executes a file operation based on the provided method.
     * @param {string} method - The operation to execute.
     * @param {string} filename - The name of the file to operate on.
     * @param {*} contents - The contents for operations that require data.
     * @returns {Promise} A promise that resolves with the operation result.
     */
    execute(method, filename, contents) {
        const self = this;

        const implementor = {
            "clearProject": () => self.clearProject(),
            "loadProject": () => self.loadProject(contents),
            "serializeProject": () => self.serializeProject(),
            "getItemNames": () => self.getItemNames(),
            "getItem": () => self.getItem(filename),
            "updateItem": () => self.updateItem(filename, contents),
            "getMbUsed": () => self.getMbUsed(),
            "createItem": () => self.createItem(filename),
            "removeItem": () => self.removeItem(filename)
        }[method];

        return implementor();
    }

    /**
     * Clears all files in the project.
     * @returns {Promise} A promise that resolves when the project is cleared.
     */
    clearProject() {
        const self = this;
        return self._inner.clearProject();
    }

    /**
     * Loads multiple files into the project.
     * @param {Object} contents - Map of filenames to file contents.
     * @returns {Promise} A promise that resolves when all files are loaded.
     */
    loadProject(contents) {
        const self = this;
        return self._inner.loadProject(contents);
    }

    /**
     * Serializes all project files into a single object.
     * @returns {Promise<Object>} A promise that resolves with a map of filenames to contents.
     */
    serializeProject() {
        const self = this;
        return self._inner.serializeProject();
    }

    /**
     * Gets the names of all files in the project.
     * @returns {Promise<Array<string>>} A promise that resolves with an array of filenames.
     */
    getItemNames() {
        const self = this;
        return self._inner.getItemNames();
    }

    /**
     * Gets the contents of a specific file.
     * @param {string} filename - The name of the file to retrieve.
     * @returns {Promise<string>} A promise that resolves with the file contents.
     */
    getItem(filename) {
        const self = this;
        return self._inner.getItem(filename);
    }

    /**
     * Updates the contents of a specific file.
     * @param {string} filename - The name of the file to update.
     * @param {string} contents - The new contents of the file.
     * @returns {Promise} A promise that resolves when the file is updated.
     */
    updateItem(filename, contents) {
        const self = this;
        return self._inner.updateItem(filename, contents);
    }

    /**
     * Gets the total storage space used by the project in megabytes.
     * @returns {Promise<number>} A promise that resolves with the storage space used.
     */
    getMbUsed() {
        const self = this;
        return self._inner.getMbUsed();
    }

    /**
     * Creates a new empty file.
     * @param {string} filename - The name of the file to create.
     * @returns {Promise} A promise that resolves when the file is created.
     */
    createItem(filename) {
        const self = this;
        return self._inner.createItem(filename);
    }

    /**
     * Removes a file from the project.
     * @param {string} filename - The name of the file to remove.
     * @returns {Promise} A promise that resolves when the file is removed.
     */
    removeItem(filename) {
        const self = this;
        return self._inner.removeItem(filename);
    }
}

class OpfsFileManager {

    constructor() {
        const self = this;
        self._fileLocks = {}
    }

    clearProject() {
        const self = this;

        return self.getItemNames().then((itemNames) => {
            const removePromises = itemNames.map((name) => self.removeItem(name));
            return Promise.all(removePromises);
        });
    }

    loadProject(contents) {
        const self = this;

        const names = Object.keys(contents);
        const futures = names.map((name) => {
            const content = contents[name];
            return self.updateItem(name, content);
        });
        return Promise.all(futures);
    }

    serializeProject() {
        const self = this;

        return self.getItemNames()
            .then((itemNames) => {
                return itemNames.map((name) => {
                    return self.getItem(name).then((content) => {
                        return {"name": name, "content": content};
                    });
                });
            })
            .then((contentFutures) => {
                return Promise.all(contentFutures);
            })
            .then((contents) => {
                const outputObj = {};
                contents.forEach((item) => {
                    outputObj[item["name"]] = item["content"];
                });
                return outputObj;
            });
    }

    getItemNames() {
        const self = this;

        const directoryFuture = navigator.storage.getDirectory();
        return directoryFuture.then((directory) => {
            return Array.fromAsync(directory.keys());
            return names;
        });
    }

    getItem(filenameRaw) {
        const self = this;

        const filename = self._cleanFilename(filenameRaw);

        return self._aquireLock(filename)
            .then(() => navigator.storage.getDirectory())
            .then((directory) => directory.getFileHandle(filename))
            .then((fileHandle) => fileHandle.getFile())
            .then((file) => file.text())
            .then((text) => {
                return self._releaseLock(filename).then(() => text);
            });
    }

    updateItem(filenameRaw, contents) {
        const self = this;
        const filename = self._cleanFilename(filenameRaw);
        return self._updateItemSync(filename, contents);
    }

    _cleanFilename(filenameRaw) {
        const self = this;
        return filenameRaw.replaceAll("%20", " ");
    }

    _updateItemSync(filename, contents) {
        const self = this;

        return self._aquireLock(filename)
            .then(() => navigator.storage.getDirectory())
            .then((directory) => directory.getFileHandle(filename, {create: true}))
            .then((asyncFile) => {
                return asyncFile.createSyncAccessHandle();
            })
            .then((syncFile) => {
                const encoder = new TextEncoder();
                const contentsEncoded = encoder.encode(contents);

                syncFile.truncate(0);
                syncFile.write(contentsEncoded);
                syncFile.flush();

                return syncFile
            })
            .then((syncFile) => syncFile.close())
            .then(() => self._releaseLock(filename));
    }

    _updateItemAsync(filename, contents) {
        const self = this;

        return self._aquireLock(filename)
            .then(() => navigator.storage.getDirectory())
            .then((directory) => directory.getFileHandle(filename, {create: true}))
            .then((fileHandle) => fileHandle.createWritable())
            .then((writable) => {
                return writable.write(contents).then(() => writable);
            })
            .then((stream) => stream.close())
            .then(() => self._releaseLock(filename));
    }

    getMbUsed() {
        const self = this;

        return self.getItemNames()
            .then((itemNames) => {
                const itemContentFutures = itemNames.map((itemName) => self.getItem(itemName));
                return Promise.all(itemContentFutures);
            })
            .then((contents) => {
                const totalSize = contents.map((x) => x.length).reduce((a, b) => a + b, 0);
                const totalKb = totalSize / 1024;
                const totalMb = totalKb / 1024;
                return totalMb;
            });
    }

    createItem(filenameRaw) {
        const self = this;

        const filename = self._cleanFilename(filenameRaw);

        return self._aquireLock(filename)
            .then(() => navigator.storage.getDirectory())
            .then((directory) => directory.getFileHandle(filename, {create: true}))
            .then(() => self._releaseLock(filename));
    }

    removeItem(filenameRaw) {
        const self = this;

        const filename = self._cleanFilename(filenameRaw);

        return self._aquireLock(filename)
            .then(() => navigator.storage.getDirectory())
            .then((directory) => {
                return directory.removeEntry(filename);
            })
            .then(() => self._releaseLock(filename));
    }

    _aquireLock(filename) {
        const self = this;
        if (self._fileLocks[filename] !== undefined) {
            const promise = self._fileLocks[filename]["promise"];
            if (promise === null) {
                return new Promise((resolve) => {
                    setTimeout(() => {
                        self._aquireLock(filename).then(resolve);
                    }, 100);
                });
            } else {
                return promise.then(() => {
                    return self._aquireLock(filename);
                });
            }
        }

        self._fileLocks[filename] = {
            "promise": null,
            "release": null
        };
        const promise = new Promise((resolve) => {
            self._fileLocks[filename]["release"] = resolve;
        });
        self._fileLocks[filename]["promise"] = promise;
        return new Promise((resolve) => resolve());
    }

    _releaseLock(filename) {
        const self = this;
        if (self._fileLocks[filename] === undefined) {
            return;
        }

        const lock = self._fileLocks[filename];
        self._fileLocks[filename] = undefined;

        const release = lock["release"];
        if (release === null) {
            return new Promise((resolve) => {
                setTimeout(() => {
                    self._releaseLock(filename).then(resolve);
                }, 100);
            });
        } else {
            return new Promise((resolve) => {
                release();
                resolve();
            });
        }
    }

}

const fileManager = new ReceiverFileManagerDecorator(
    new OpfsFileManager()
);

self.onmessage = (message) => {
    const data = message.data;
    const messageId = data["messageId"];
    const method = data["method"];
    const filename = data["filename"];
    const contents = data["contents"];

    fileManager.execute(method, filename, contents).then((result) => {
        self.postMessage({
            "messageId": messageId,
            "return": result
        });
    });
};