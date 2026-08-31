const API_BASE = "/file";

const elements = {
  apiKey: document.getElementById("apiKey"),
  fileDropArea: document.getElementById("fileDropArea"),
  fileInfo: document.getElementById("fileInfo"),
  fileInput: document.getElementById("fileInput"),
  fileList: document.getElementById("fileList"),
  refreshHeroButton: document.getElementById("refreshHeroButton"),
  refreshListButton: document.getElementById("refreshListButton"),
  responseBody: document.getElementById("responseBody"),
  responseDuration: document.getElementById("responseDuration"),
  responseEndpoint: document.getElementById("responseEndpoint"),
  responseMeta: document.getElementById("responseMeta"),
  responseMethod: document.getElementById("responseMethod"),
  responseStatus: document.getElementById("responseStatus"),
  themeToggle: document.getElementById("themeToggle"),
  toggleApiKey: document.getElementById("toggleApiKey"),
  uploadButton: document.getElementById("uploadButton"),
  uploadForm: document.getElementById("uploadForm")
};

function headers() {
  return { "X-API-KEY": elements.apiKey.value.trim() };
}

function setBusy(isBusy) {
  document.body.classList.toggle("is-busy", isBusy);
  document.querySelectorAll("button").forEach((button) => {
    if (button !== elements.themeToggle && button !== elements.toggleApiKey) {
      button.disabled = isBusy;
    }
  });
}

function showResponse(method, endpoint, status, body, duration) {
  const numericStatus = Number(status);
  const statusKind = Number.isFinite(numericStatus)
    ? (numericStatus >= 200 && numericStatus < 300 ? "success" : "error")
    : "error";

  elements.responseMeta.hidden = false;
  elements.responseMethod.textContent = method;
  elements.responseMethod.dataset.method = method.toLowerCase();
  elements.responseEndpoint.textContent = endpoint;
  elements.responseDuration.textContent = duration === undefined ? "" : `${duration} ms`;
  elements.responseStatus.textContent = String(status);
  elements.responseStatus.className = `response-status ${statusKind}`;
  elements.responseBody.textContent = typeof body === "string" ? body : JSON.stringify(body, null, 2);
}

async function readBody(response) {
  const text = await response.text();
  if (!text) {
    return "(empty response)";
  }
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

async function apiRequest(method, endpoint, options = {}) {
  const started = performance.now();
  const response = await fetch(endpoint, {
    method,
    headers: { ...headers(), ...(options.headers || {}) },
    body: options.body
  });
  const body = await readBody(response);
  if (options.showResponse !== false || !response.ok) {
    showResponse(method, endpoint, response.status, body, Math.round(performance.now() - started));
  }
  return { response, body };
}

function emptyList(message) {
  const item = document.createElement("li");
  item.className = "empty-state";
  item.textContent = message;
  elements.fileList.replaceChildren(item);
}

function actionButton(label, className, handler) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `file-action ${className}`;
  button.textContent = label;
  button.addEventListener("click", handler);
  return button;
}

function renderFiles(files) {
  if (!Array.isArray(files) || files.length === 0) {
    emptyList("No files stored yet.");
    return;
  }

  const items = files.map((filename) => {
    const item = document.createElement("li");
    const nameBlock = document.createElement("div");
    const icon = document.createElement("span");
    const name = document.createElement("span");
    const actions = document.createElement("div");

    nameBlock.className = "file-name";
    icon.className = "file-icon";
    icon.textContent = "FILE";
    name.textContent = filename;
    actions.className = "file-actions";
    actions.append(
      actionButton("Info", "info", () => fetchMetadata(filename)),
      actionButton("Download", "download", () => downloadFile(filename)),
      actionButton("Delete", "delete", () => deleteFile(filename))
    );
    nameBlock.append(icon, name);
    item.append(nameBlock, actions);
    return item;
  });

  elements.fileList.replaceChildren(...items);
}

async function refreshFiles(showResponse = true) {
  setBusy(true);
  try {
    const { response, body } = await apiRequest("GET", `${API_BASE}/list`, { showResponse });
    if (!response.ok) {
      emptyList(`Unable to load files (HTTP ${response.status}).`);
      return;
    }
    renderFiles(body);
  } catch (error) {
    showResponse("GET", `${API_BASE}/list`, "NETWORK ERROR", error.message);
    emptyList("The API could not be reached.");
  } finally {
    setBusy(false);
  }
}

async function fetchMetadata(filename) {
  const endpoint = `${API_BASE}/info/${encodeURIComponent(filename)}`;
  setBusy(true);
  try {
    await apiRequest("GET", endpoint);
  } catch (error) {
    showResponse("GET", endpoint, "NETWORK ERROR", error.message);
  } finally {
    setBusy(false);
  }
}

async function downloadFile(filename) {
  const endpoint = `${API_BASE}/${encodeURIComponent(filename)}`;
  const started = performance.now();
  setBusy(true);
  try {
    const response = await fetch(endpoint, { headers: headers() });
    const duration = Math.round(performance.now() - started);
    if (!response.ok) {
      showResponse("GET", endpoint, response.status, await readBody(response), duration);
      return;
    }

    const blob = await response.blob();
    showResponse("GET", endpoint, response.status, {
      message: "Download ready",
      filename,
      bytes: blob.size,
      contentType: blob.type || "application/octet-stream"
    }, duration);

    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = filename;
    link.hidden = true;
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
  } catch (error) {
    showResponse("GET", endpoint, "NETWORK ERROR", error.message);
  } finally {
    setBusy(false);
  }
}

async function deleteFile(filename) {
  if (!window.confirm(`Delete ${filename}? This cannot be undone.`)) {
    return;
  }

  const endpoint = `${API_BASE}/${encodeURIComponent(filename)}`;
  setBusy(true);
  try {
    const { response, body } = await apiRequest("DELETE", endpoint);
    if (response.ok && body === true) {
      await refreshFiles(false);
    }
  } catch (error) {
    showResponse("DELETE", endpoint, "NETWORK ERROR", error.message);
  } finally {
    setBusy(false);
  }
}

elements.uploadForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const file = elements.fileInput.files[0];
  if (!file) {
    showResponse("POST", API_BASE, "INPUT ERROR", "Choose a file before uploading.");
    return;
  }

  const formData = new FormData();
  formData.append("file", file);
  setBusy(true);
  try {
    const { response } = await apiRequest("POST", API_BASE, { body: formData });
    if (response.ok) {
      elements.uploadForm.reset();
      elements.fileInfo.hidden = true;
      await refreshFiles(false);
    }
  } catch (error) {
    showResponse("POST", API_BASE, "NETWORK ERROR", error.message);
  } finally {
    setBusy(false);
  }
});

elements.fileInput.addEventListener("change", () => {
  const file = elements.fileInput.files[0];
  if (!file) {
    elements.fileInfo.hidden = true;
    return;
  }
  elements.fileInfo.textContent = `${file.name} · ${(file.size / 1024).toFixed(1)} KB · ${file.type || "unknown type"}`;
  elements.fileInfo.hidden = false;
});

elements.fileDropArea.addEventListener("dragover", (event) => {
  event.preventDefault();
  elements.fileDropArea.classList.add("drag-over");
});

elements.fileDropArea.addEventListener("dragleave", () => elements.fileDropArea.classList.remove("drag-over"));
elements.fileDropArea.addEventListener("drop", (event) => {
  event.preventDefault();
  elements.fileDropArea.classList.remove("drag-over");
  if (event.dataTransfer.files.length > 0) {
    elements.fileInput.files = event.dataTransfer.files;
    elements.fileInput.dispatchEvent(new Event("change"));
  }
});
elements.refreshHeroButton.addEventListener("click", refreshFiles);
elements.refreshListButton.addEventListener("click", refreshFiles);

elements.toggleApiKey.addEventListener("click", () => {
  const shouldShow = elements.apiKey.type === "password";
  elements.apiKey.type = shouldShow ? "text" : "password";
  elements.toggleApiKey.textContent = shouldShow ? "Hide" : "Show";
  elements.toggleApiKey.setAttribute("aria-label", `${shouldShow ? "Hide" : "Show"} API key`);
});

elements.themeToggle.addEventListener("click", () => {
  const root = document.documentElement;
  const light = root.dataset.theme === "light";
  root.dataset.theme = light ? "dark" : "light";
  elements.themeToggle.setAttribute("aria-label", `Switch to ${light ? "light" : "dark"} theme`);
});

refreshFiles();
