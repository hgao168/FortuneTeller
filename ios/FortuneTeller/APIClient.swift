import Foundation
import UIKit

enum APIError: LocalizedError {
    case server(String)
    case invalidImage
    case badResponse
    case network(URLError, host: String)
    case transport(String)

    var errorDescription: String? {
        switch self {
        case .server(let msg): return msg
        case .invalidImage: return "Could not read the selected image."
        case .badResponse: return "Unexpected server response."
        case .network(let error, let host):
            switch error.code {
            case .notConnectedToInternet:
                return "No internet connection. Please connect and try again."
            case .timedOut:
                return "The request timed out while contacting \(host)."
            case .cannotFindHost, .cannotConnectToHost, .dnsLookupFailed:
                return "Could not reach \(host). Please try again in a moment."
            case .secureConnectionFailed, .serverCertificateHasBadDate, .serverCertificateUntrusted:
                return "A secure connection to \(host) could not be established."
            default:
                return "Network error (\(error.code.rawValue)) while contacting \(host)."
            }
        case .transport(let msg):
            return msg
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
        form.appendField("language", value: LanguageRuntime.currentAPILanguageCode)
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
        form.appendField("language", value: LanguageRuntime.currentAPILanguageCode)
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
        let requestURL = baseURL.appendingPathComponent(path)
        var request = URLRequest(url: requestURL)
        request.httpMethod = "POST"
        request.timeoutInterval = 60
        request.setValue(form.contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = form.finalize()

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch let error as URLError {
            throw APIError.network(error, host: requestURL.host ?? baseURL.host ?? "server")
        } catch {
            throw APIError.transport("Request failed: \(error.localizedDescription)")
        }
        guard let http = response as? HTTPURLResponse else { throw APIError.badResponse }
        guard (200..<300).contains(http.statusCode) else {
            let message = String(data: data, encoding: .utf8) ?? "Server returned \(http.statusCode)"
            throw APIError.server(message)
        }
        return try JSONDecoder().decode(T.self, from: data)
    }
}
