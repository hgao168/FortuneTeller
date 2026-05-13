import Foundation
import UIKit

final class PalmStore: ObservableObject {
    @Published private(set) var readings: [SavedReading] = []

    private let storageKey = "com.fortuneteller.saved_readings"

    init() { load() }

    func save(_ reading: SavedReading) {
        readings.insert(reading, at: 0)
        persist()
    }

    func delete(at offsets: IndexSet) {
        readings.remove(atOffsets: offsets)
        persist()
    }

    // MARK: - Private

    private func persist() {
        guard let data = try? JSONEncoder().encode(readings) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }

    private func load() {
        guard
            let data = UserDefaults.standard.data(forKey: storageKey),
            let decoded = try? JSONDecoder().decode([SavedReading].self, from: data)
        else { return }
        readings = decoded
    }
}

// MARK: - SavedReading + UIKit helpers

extension SavedReading {
    var thumbnailImage: UIImage? {
        guard let data = thumbnailData else { return nil }
        return UIImage(data: data)
    }

    var secondaryThumbnailImage: UIImage? {
        guard let data = secondaryThumbnailData else { return nil }
        return UIImage(data: data)
    }
}
