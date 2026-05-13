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
        if let raw = Bundle.main.object(forInfoDictionaryKey: "BACKEND_BASE_URL") as? String {
            let urlString = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            if !urlString.isEmpty {
                if let url = URL(string: urlString), url.scheme != nil {
                    return url
                }
                if let url = URL(string: "https://\(urlString)") {
                    return url
                }
            }
        }
        // Local default for first-run developer ergonomics.
        return URL(string: "http://localhost:8000")!
    }()

    func analyzePalm(image: UIImage, scope: ReadingScope, userID: String? = nil) async throws -> PalmReadingResponse {
        guard let jpeg = image.jpegData(compressionQuality: 0.85) else {
            throw APIError.invalidImage
        }

        var form = MultipartFormData()
        form.appendField("scope", value: scope.rawValue)
        if let userID, !userID.isEmpty {
            form.appendField("user_id", value: userID)
        }
        form.appendFile("image", filename: "palm.jpg", data: jpeg)

        return try await postMultipart(path: "analyze-palm", form: form)
    }

    func matchPalm(
        imageA: UIImage,
        imageB: UIImage,
        matchType: MatchType,
        personABirth: Date,
        personBBirth: Date
    ) async throws -> PalmMatchResponse {
        guard
            let jpegA = imageA.jpegData(compressionQuality: 0.85),
            let jpegB = imageB.jpegData(compressionQuality: 0.85)
        else {
            throw APIError.invalidImage
        }

        var form = MultipartFormData()
        form.appendField("match_type", value: matchType.rawValue)
        form.appendField("person_a_birth", value: APIClient.isoFormatter.string(from: personABirth))
        form.appendField("person_b_birth", value: APIClient.isoFormatter.string(from: personBBirth))
        form.appendFile("image_a", filename: "palm-a.jpg", data: jpegA)
        form.appendFile("image_b", filename: "palm-b.jpg", data: jpegB)

        return try await postMultipart(path: "match-palm", form: form)
    }

    // MARK: - Private

    private static let isoFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    private func postMultipart<T: Decodable>(path: String, form: MultipartFormData) async throws -> T {
        var request = URLRequest(url: baseURL.appendingPathComponent(path))
        request.httpMethod = "POST"
        request.timeoutInterval = 60
        request.setValue(form.contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = form.finalize()

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw APIError.badResponse }
        guard (200..<300).contains(http.statusCode) else {
            let message = String(data: data, encoding: .utf8) ?? "Server returned \(http.statusCode)"
            throw APIError.server(message)
        }
        return try JSONDecoder().decode(T.self, from: data)
    }
}
