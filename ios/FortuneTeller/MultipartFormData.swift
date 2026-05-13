import Foundation

/// Lightweight RFC 7578 multipart/form-data builder.
///
/// Centralizes boundary generation, field/file encoding, and force-unwrap
/// noise that previously lived inline in `APIClient`.
struct MultipartFormData {
    let boundary: String
    private var body = Data()

    init(boundary: String = "Boundary-\(UUID().uuidString)") {
        self.boundary = boundary
    }

    var contentType: String { "multipart/form-data; boundary=\(boundary)" }

    mutating func appendField(_ name: String, value: String) {
        appendBoundary()
        appendLine("Content-Disposition: form-data; name=\"\(name)\"")
        appendLine("")
        appendLine(value)
    }

    mutating func appendFile(
        _ name: String,
        filename: String,
        mimeType: String = "image/jpeg",
        data: Data
    ) {
        appendBoundary()
        appendLine("Content-Disposition: form-data; name=\"\(name)\"; filename=\"\(filename)\"")
        appendLine("Content-Type: \(mimeType)")
        appendLine("")
        body.append(data)
        appendLine("")
    }

    func finalize() -> Data {
        var finalBody = body
        finalBody.append(asciiData("--\(boundary)--\r\n"))
        return finalBody
    }

    // MARK: - Private

    private mutating func appendBoundary() {
        body.append(asciiData("--\(boundary)\r\n"))
    }

    private mutating func appendLine(_ line: String) {
        body.append(asciiData("\(line)\r\n"))
    }

    private func asciiData(_ string: String) -> Data {
        // UTF-8 of ASCII content is always non-nil; the fallback keeps the
        // call site free of force-unwraps.
        string.data(using: .utf8) ?? Data()
    }
}
