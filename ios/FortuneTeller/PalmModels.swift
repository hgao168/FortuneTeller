import Foundation
import SwiftUI

enum ReadingScope: String, CaseIterable, Identifiable, Codable {
    case today
    case month
    case year
    case longTerm = "long_term"

    var id: String { rawValue }

    var localizedKey: LocalizedStringKey {
        switch self {
        case .today:    return "scope.today"
        case .month:    return "scope.month"
        case .year:     return "scope.year"
        case .longTerm: return "scope.longterm"
        }
    }
}

struct PalmReadingSection: Codable, Identifiable, Hashable {
    var id: String { title }
    let title: String
    let text: String
}

struct PalmReadingResponse: Codable, Hashable {
    let scope: ReadingScope
    let summary: String
    let sections: [PalmReadingSection]
    let advice: [String]
    let disclaimer: String
}

enum MatchType: String, CaseIterable, Identifiable, Codable {
    case romantic
    case friend

    var id: String { rawValue }

    var localizedKey: LocalizedStringKey {
        switch self {
        case .romantic: return "match.romantic"
        case .friend: return "match.friend"
        }
    }
}

struct PalmMatchResponse: Codable, Hashable {
    let matchType: MatchType
    let score: Int
    let summary: String
    let strengths: [String]
    let tensions: [String]
    let advice: [String]
    let disclaimer: String

    enum CodingKeys: String, CodingKey {
        case matchType = "match_type"
        case score
        case summary
        case strengths
        case tensions
        case advice
        case disclaimer
    }
}

enum SavedReadingContent: Codable, Hashable {
    case palm(PalmReadingResponse)
    case match(PalmMatchResponse)
}

struct SavedReading: Codable, Identifiable, Hashable {
    var id: UUID
    var date: Date
    var content: SavedReadingContent
    /// Primary thumbnail: palm image (palm reading) or person-A image (match).
    var thumbnailData: Data?
    /// Secondary thumbnail used only for match readings (person B).
    var secondaryThumbnailData: Data?

    init(
        id: UUID = UUID(),
        date: Date = .now,
        palm: PalmReadingResponse,
        thumbnailData: Data? = nil
    ) {
        self.id = id
        self.date = date
        self.content = .palm(palm)
        self.thumbnailData = thumbnailData
        self.secondaryThumbnailData = nil
    }

    init(
        id: UUID = UUID(),
        date: Date = .now,
        match: PalmMatchResponse,
        thumbnailA: Data? = nil,
        thumbnailB: Data? = nil
    ) {
        self.id = id
        self.date = date
        self.content = .match(match)
        self.thumbnailData = thumbnailA
        self.secondaryThumbnailData = thumbnailB
    }

    var summary: String {
        switch content {
        case .palm(let r): return r.summary
        case .match(let r): return r.summary
        }
    }

    var titleKey: LocalizedStringKey {
        switch content {
        case .palm(let r): return r.scope.localizedKey
        case .match(let r): return r.matchType.localizedKey
        }
    }
}
