import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var store: PalmStore
    @State private var selected: SavedReading?

    var body: some View {
        NavigationStack {
            ZStack {
                FutureBackground()

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
                                .listRowInsets(EdgeInsets(top: 8, leading: 12, bottom: 8, trailing: 12))
                                .listRowBackground(
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                        .fill(Color.white.opacity(0.72))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                                .stroke(Color.orange.opacity(0.25), lineWidth: 1)
                                        )
                                )
                            }
                            .onDelete { offsets in
                                store.delete(at: offsets)
                            }
                        }
                        .listStyle(.plain)
                        .scrollContentBackground(.hidden)
                    }
                }
            }
            .navigationTitle(Text("nav.history"))
            .navigationDestination(isPresented: isShowingSelection) {
                if let reading = selected {
                    destinationView(for: reading)
                } else {
                    EmptyView()
                }
            }
        }
    }

    private var isShowingSelection: Binding<Bool> {
        Binding(
            get: { selected != nil },
            set: { isPresented in
                if !isPresented {
                    selected = nil
                }
            }
        )
    }

    @ViewBuilder
    private func destinationView(for reading: SavedReading) -> some View {
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

    private var emptyState: some View {
        VStack(spacing: 16) {
            Text("🫳")
                .font(.system(size: 52))
            Text("history.empty")
                .font(.body)
                .foregroundStyle(Color(red: 0.28, green: 0.20, blue: 0.36))
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
                            .foregroundColor(Color(red: 0.94, green: 0.44, blue: 0.34))
                    }
                }
                Text(reading.summary)
                    .font(.caption)
                    .foregroundStyle(Color(red: 0.33, green: 0.28, blue: 0.40))
                    .lineLimit(2)
                Text(reading.date.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption2)
                    .foregroundStyle(Color(red: 0.44, green: 0.36, blue: 0.52))
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
                Color.pink.opacity(0.22)
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
            .background(Color.orange.opacity(0.18))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .foregroundColor(Color(red: 0.94, green: 0.44, blue: 0.34))
    }
}
