import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var store: PalmStore
    @State private var selected: SavedReading?

    var body: some View {
        NavigationStack {
            Group {
                if store.readings.isEmpty {
                    emptyState
                } else {
                    List {
                        ForEach(store.readings) { reading in
                            Button {
                                selected = reading
                            } label: {
                                HistoryRowView(reading: reading)
                            }
                            .foregroundStyle(.primary)
                        }
                        .onDelete { offsets in
                            store.delete(at: offsets)
                        }
                    }
                }
            }
            .navigationTitle(Text("nav.history"))
            .navigationDestination(item: $selected) { reading in
                switch reading.content {
                case .palm(let response):
                    ReadingResultView(response: response, palmImage: reading.thumbnailImage)
                case .match(let response):
                    MatchResultView(
                        response: response,
                        imageA: reading.thumbnailImage,
                        imageB: reading.secondaryThumbnailImage
                    )
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "hand.raised.slash")
                .font(.system(size: 52))
                .foregroundStyle(.secondary)
            Text("history.empty")
                .font(.body)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct HistoryRowView: View {
    let reading: SavedReading

    var body: some View {
        HStack(spacing: 12) {
            thumbnail
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(reading.titleKey)
                        .font(.subheadline.weight(.semibold))
                    if case .match(let r) = reading.content {
                        Text("\(r.score)%")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.purple)
                    }
                }
                Text(reading.summary)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                Text(reading.date.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private var thumbnail: some View {
        switch reading.content {
        case .palm:
            if let img = reading.thumbnailImage {
                Image(uiImage: img)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 56, height: 56)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            } else {
                placeholderIcon(systemName: "hand.raised.fill")
            }
        case .match:
            HStack(spacing: -10) {
                matchThumb(reading.thumbnailImage)
                matchThumb(reading.secondaryThumbnailImage)
            }
            .frame(width: 56, height: 56)
        }
    }

    private func matchThumb(_ image: UIImage?) -> some View {
        Group {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Color.purple.opacity(0.15)
            }
        }
        .frame(width: 36, height: 36)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color(.systemBackground), lineWidth: 2))
    }

    private func placeholderIcon(systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.title2)
            .frame(width: 56, height: 56)
            .background(Color.purple.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .foregroundColor(.purple)
    }
}
