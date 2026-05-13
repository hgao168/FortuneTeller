import Foundation
import UIKit

enum APIError: LocalizedError {
    case server(String)
    case invalidImage
    case badResponse

    var errorDescription: String? {
        switch self {
        case .server(let msg): return msg
        case .invalidImage: return "Could not read the selected image."
        case .badResponse: return "Unexpected server response."
        }
    }
}

final class APIClient {
    static let shared = APIClient()

    private let baseURL: URL = {
        if let urlString = Bundle.main.object(forInfoDictionaryKey: "BACKEND_BASE_URL") as? String,
           !urlString.isEmpty,
           let url = URL(string: urlString) {
            return url
        }
        // Local default for first-run developer ergonomics.
        return URL(string: "http://localhost:8000")!
    }()

    func analyzePalm(image: UIImage, scope: ReadingScope, userID: String? = nil) async throws -> PalmReadingResponse {
        guard let jpeg = image.jpegData(compressionQuality: 0.85) else {
            throw APIError.invalidImage
        }

        let endpoint = baseURL.appendingPathComponent("analyze-palm")
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.timeoutInterval = 60

        let boundary = "Boundary-\(UUID().uuidString)"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        func appendField(_ name: String, _ value: String) {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(value)\r\n".data(using: .utf8)!)
        }

        appendField("scope", scope.rawValue)
        if let userID, !userID.isEmpty { appendField("user_id", userID) }

        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"image\"; filename=\"palm.jpg\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(jpeg)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

        request.httpBody = body

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw APIError.badResponse }
        guard (200..<300).contains(http.statusCode) else {
            let message = String(data: data, encoding: .utf8) ?? "Server returned \(http.statusCode)"
            throw APIError.server(message)
        }
        return try JSONDecoder().decode(PalmReadingResponse.self, from: data)
    }
}
