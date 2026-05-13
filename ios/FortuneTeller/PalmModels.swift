import Foundation

enum ReadingScope: String, CaseIterable, Identifiable, Codable {
    case year
    case longTerm = "long_term"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .year: return "This Year"
        case .longTerm: return "Long Term"
        }
    }
}

struct PalmReadingSection: Codable, Identifiable, Hashable {
    var id: String { title }
    let title: String
    let text: String
}

struct PalmReadingResponse: Codable {
    let scope: ReadingScope
    let summary: String
    let sections: [PalmReadingSection]
    let advice: [String]
    let disclaimer: String
}
