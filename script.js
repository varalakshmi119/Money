// Configure PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc =
  "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.4.120/pdf.worker.min.js";

// Constants
const CONSTANTS = {
  MAX_FILE_SIZE: 10 * 1024 * 1024, // 10MB
  VALID_PHONE_PATTERN: /^[6-9]\d{9}$/,
  DATE_PATTERN:
    /^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+\d{1,2},\s+\d{4}$/i,
  TIME_PATTERN: /^\d{1,2}:\d{2}\s+(?:AM|PM)$/i,
  LOOK_AHEAD_LINES: 10,
  INR_PATTERN: /(?:INR|₹)/i,
  NUMBER_PATTERN: /^[\d,]+\.?\d{0,2}$/,
  VALID_STATEMENT_KEYWORDS: [
    "Transaction Statement",
    "Transaction ID",
    "UTR No",
  ],
};

// Utility Functions
class Utils {
  static formatFileSize(bytes) {
    return (bytes / (1024 * 1024)).toFixed(2);
  }

  static createDownloadLink(content, filename) {
    const blob = new Blob([content], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    return { element: a, url };
  }
}

// PDF Handler
class PDFHandler {
  constructor(domHandler) {
    this.domHandler = domHandler;
  }

  async extractContent(file) {
    const arrayBuffer = await file.arrayBuffer();
    return new Promise((resolve, reject) => {
      const loadingTask = pdfjsLib.getDocument({
        data: arrayBuffer,
        password: this.getPassword(),
      });

      loadingTask.promise
        .then((pdf) => this.extractText(pdf))
        .then(resolve)
        .catch((error) => {
          if (error.name === "PasswordException") {
            this.domHandler.showPasswordInput();
            reject(new Error("PDF is password protected"));
          } else {
            reject(error);
          }
        });
    });
  }

  getPassword() {
    const passwordInput = this.domHandler.elements.pdfPassword;
    const phoneInput = this.domHandler.elements.phoneNumber;

    if (!passwordInput || !phoneInput) {
      console.error("Input elements not found");
      return "";
    }

    return (
      (passwordInput.value || "").trim() || (phoneInput.value || "").trim()
    );
  }

  async extractText(pdf) {
    let fullText = "";
    for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
      const page = await pdf.getPage(pageNum);
      const textContent = await page.getTextContent();
      const pageText = textContent.items.map((item) => item.str).join("\n");
      fullText += pageText + "\n";
    }

    if (!this.isValidPhonePeStatement(fullText)) {
      throw new Error(
        "The uploaded file does not appear to be a valid PhonePe statement.",
      );
    }

    return fullText;
  }

  isValidPhonePeStatement(content) {
    const contentLowerCase = content.toLowerCase();
    return CONSTANTS.VALID_STATEMENT_KEYWORDS.some((keyword) =>
      contentLowerCase.includes(keyword.toLowerCase()),
    );
  }
}

// Transaction Parser
class TransactionParser {
  static isTransactionDetail(line) {
    const lowerLine = line.toLowerCase();
    return (
      lowerLine.startsWith("paid to") ||
      lowerLine.startsWith("received from") ||
      lowerLine.startsWith("paid -")
    );
  }

  static getTransactionType(line) {
    const lowerLine = line.toLowerCase();
    if (lowerLine.startsWith("received from")) return "Credit";
    if (lowerLine.startsWith("paid to") || lowerLine.startsWith("paid -"))
      return "Debit";
    return null;
  }

  static isAccountReference(line) {
    const lowerLine = line.toLowerCase();
    return (
      lowerLine.includes("debited from") ||
      lowerLine.includes("credited to") ||
      lowerLine.includes("paid by")
    );
  }

  static extractAmount(lines, currentIndex) {
    const currentLine = lines[currentIndex];
    const nextLine = lines[currentIndex + 1];

    const normalMatch = currentLine.match(/(?:INR|₹)\s*([\d,]+\.?\d{0,2})/i);
    if (normalMatch) {
      const cleanAmount = normalMatch[1].replace(/,/g, "");
      return parseFloat(cleanAmount).toFixed(2);
    }

    if (
      CONSTANTS.INR_PATTERN.test(currentLine) &&
      nextLine &&
      CONSTANTS.NUMBER_PATTERN.test(nextLine.trim())
    ) {
      const cleanAmount = nextLine.trim().replace(/,/g, "");
      return parseFloat(cleanAmount).toFixed(2);
    }

    return "0.00";
  }

  static validateTransaction(transaction) {
    return {
      ...transaction,
      amount: transaction.amount || "0.00",
      timestamp: `${transaction.date.trim()} ${transaction.time.trim()}`,
    };
  }
}

// Main Application
class PhonePeStatementConverter {
  constructor() {
    this.dom = new DOMHandler();
    this.setupEventListeners();
  }

  setupEventListeners() {
    this.dom.elements.processBtn.addEventListener("click", () =>
      this.handleProcessClick(),
    );
    this.dom.elements.downloadBtn.addEventListener("click", () =>
      this.handleDownloadClick(),
    );
    this.dom.elements.fileInput.addEventListener("change", () =>
      this.handleFileChange(),
    );
  }

  handleFileChange() {
    const file = this.dom.elements.fileInput.files[0];
    this.dom.updateFileInfo(file);
    this.dom.hidePasswordInput();
  }

  async handleProcessClick() {
    const file = this.dom.elements.fileInput.files[0];
    if (!file) {
      this.dom.showError("Please select a PDF file to process.");
      return;
    }
    await this.processFile(file);
  }

  handleDownloadClick() {
    const jsonStr = this.dom.elements.jsonPreview.textContent;
    const { element, url } = Utils.createDownloadLink(
      jsonStr,
      "phonepe_statement.json",
    );
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
    URL.revokeObjectURL(url);
  }

  validateInputs(file) {
    if (!file.type || file.type !== "application/pdf") {
      this.dom.showError("Please upload a valid PDF file.");
      return false;
    }
    if (file.size > CONSTANTS.MAX_FILE_SIZE) {
      this.dom.showError("File size should be less than 10MB.");
      return false;
    }
    return true;
  }

  async processFile(file) {
    if (!this.validateInputs(file)) return;

    this.dom.showSpinner();
    this.dom.hideError();
    this.dom.hideResult();

    try {
      const pdfHandler = new PDFHandler(this.dom);
      const content = await pdfHandler.extractContent(file);
      const result = await this.processContent(content);
      this.dom.displayResult(result);
    } catch (error) {
      this.dom.showError(error.message);
      console.error("Error processing file:", error);
    } finally {
      this.dom.hideSpinner();
    }
  }

  async processContent(content) {
    const lines = content
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean);
    const transactions = [];
    let currentTransaction = null;

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      if (CONSTANTS.DATE_PATTERN.test(line)) {
        if (currentTransaction) {
          transactions.push(
            TransactionParser.validateTransaction(currentTransaction),
          );
        }

        currentTransaction = this.initializeTransaction(line);
        i = this.processTransactionDetails(lines, i, currentTransaction);
      }
    }

    if (currentTransaction) {
      transactions.push(
        TransactionParser.validateTransaction(currentTransaction),
      );
    }

    return { transactions };
  }

  initializeTransaction(date) {
    return {
      date: date.trim(),
      time: "",
      details: "",
      transactionId: "",
      utrNo: "",
      accountReference: "",
      type: "",
      amount: "",
    };
  }

  processTransactionDetails(lines, startIndex, transaction) {
    for (
      let j = startIndex + 1;
      j < Math.min(startIndex + CONSTANTS.LOOK_AHEAD_LINES, lines.length);
      j++
    ) {
      const line = lines[j];

      if (CONSTANTS.TIME_PATTERN.test(line)) {
        transaction.time = line.trim();
      } else if (TransactionParser.isTransactionDetail(line)) {
        transaction.details = line.trim();
        transaction.type = TransactionParser.getTransactionType(line);
      } else if (/Transaction ID/i.test(line)) {
        const transactionIdMatch = line.match(/Transaction ID[:\s]*([^\s]+)/i);
        if (transactionIdMatch) {
          transaction.transactionId = transactionIdMatch[1].trim();
        }
      } else if (/UTR No/i.test(line)) {
        const utrNoMatch = line.match(/UTR No[.:\s]*([^\s]+)/i);
        if (utrNoMatch) {
          transaction.utrNo = utrNoMatch[1].trim();
        }
      } else if (TransactionParser.isAccountReference(line)) {
        transaction.accountReference = line.replace(/\s+/g, " ").trim();
      } else if (/(?:INR|₹)/i.test(line)) {
        transaction.amount = TransactionParser.extractAmount(lines, j);
        if (
          transaction.amount !== "0.00" &&
          j + 1 < lines.length &&
          CONSTANTS.NUMBER_PATTERN.test(lines[j + 1].trim())
        ) {
          j++;
        }
      }
    }
    return startIndex + CONSTANTS.LOOK_AHEAD_LINES - 1;
  }
}

// DOM Handler
class DOMHandler {
  constructor() {
    this.elements = {
      fileInput: document.getElementById("fileInput"),
      phoneNumber: document.getElementById("phoneNumber"),
      pdfPassword: document.getElementById("pdfPassword"),
      processBtn: document.getElementById("processBtn"),
      spinner: document.getElementById("spinner"),
      statusMessage: document.getElementById("statusMessage"),
      resultContainer: document.getElementById("resultContainer"),
      jsonPreview: document.getElementById("jsonPreview"),
      downloadBtn: document.getElementById("downloadBtn"),
      fileInfo: document.getElementById("fileInfo"),
      passwordContainer: document.getElementById("passwordContainer"),
    };
  }

  updateFileInfo(file) {
    if (file) {
      const sizeMB = Utils.formatFileSize(file.size);
      this.elements.fileInfo.textContent = `Selected file: ${file.name} (${sizeMB} MB)`;
    } else {
      this.elements.fileInfo.textContent = "";
    }
  }

  showPasswordInput() {
    this.elements.passwordContainer.style.display = "block";
  }

  hidePasswordInput() {
    this.elements.passwordContainer.style.display = "none";
  }

  showSpinner() {
    this.elements.spinner.style.display = "block";
  }

  hideSpinner() {
    this.elements.spinner.style.display = "none";
  }

  showError(message) {
    this.elements.statusMessage.textContent = message;
    this.elements.statusMessage.className = "status-message error";
    this.elements.statusMessage.style.display = "block";
  }

  hideError() {
    this.elements.statusMessage.style.display = "none";
  }

  hideResult() {
    this.elements.resultContainer.style.display = "none";
  }

  displayResult(result) {
    this.elements.jsonPreview.textContent = JSON.stringify(result, null, 2);
    this.elements.resultContainer.style.display = "block";
  }
}

// Initialize the application
document.addEventListener("DOMContentLoaded", () => {
  new PhonePeStatementConverter();
});
