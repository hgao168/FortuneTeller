import SwiftUI

struct ReadingResultView: View {
    let response: PalmReadingResponse
    let palmImage: UIImage?

    @EnvironmentObject var store: PalmStore
    @State private var saved = false

    var body: some View {
        ZStack {
            FutureBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    hero

                    Text(response.summary)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(.white)
                        .lineSpacing(4)
                        .padding(.horizontal, 2)

                    ForEach(response.sections) { section in
                        resultSection(section)
                    }

                    if !response.advice.isEmpty {
                        adviceCard
                    }

                    Text(response.disclaimer)
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.55))
                        .lineSpacing(3)
                        .padding(.top, 6)
                }
                .padding(20)
            }
        }
        .navigationTitle(Text("nav.reading"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    let thumb = palmImage?.jpegData(compressionQuality: 0.5)
                    store.save(SavedReading(palm: response, thumbnailData: thumb))
                    saved = true
                } label: {
                    Label(
                        saved ? LanguageRuntime.localized("button.saved") : LanguageRuntime.localized("button.save"),
                        systemImage: saved ? "checkmark.circle.fill" : "square.and.arrow.down"
                    )
                    .foregroundStyle(.white)
                }
                .disabled(saved)
            }
        }
    }

    private var hero: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 14) {
                if let image = palmImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(height: 235)
                        .frame(maxWidth: .infinity)
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .stroke(.white.opacity(0.18), lineWidth: 1)
                        )
                }

                HStack {
                    Label(response.scope.localizedKey, systemImage: "sparkles")
                        .font(.subheadline.weight(.bold))
                        .padding(.vertical, 8)
                        .padding(.horizontal, 12)
                        .foregroundStyle(.white)
                        .background(.white.opacity(0.12), in: Capsule())

                    Spacer()
                }

                Text("result.cosmic.signal")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.cyan.opacity(0.9))
                    .textCase(.uppercase)
            }
        }
    }

    private func resultSection(_ section: PalmReadingSection) -> some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 10) {
                    Image(systemName: icon(for: section.title))
                        .foregroundStyle(.pink)
                    Text(section.title)
                        .font(.headline)
                        .foregroundStyle(.white)
                }

                Text(section.text)
                    .font(.body)
                    .foregroundStyle(.white.opacity(0.72))
                    .lineSpacing(4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var adviceCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 12) {
                Label("section.advice", systemImage: "lightbulb.max.fill")
                    .font(.headline)
                    .foregroundStyle(.white)

                ForEach(response.advice, id: \.self) { item in
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "checkmark.seal.fill")
                            .foregroundStyle(.cyan)
                            .padding(.top, 2)
                        Text(item)
                            .font(.body)
                            .foregroundStyle(.white.opacity(0.75))
                            .lineSpacing(3)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func icon(for title: String) -> String {
        let lower = title.lowercased()
        if lower.contains("career") || lower.contains("事业") { return "briefcase.fill" }
        if lower.contains("wealth") || lower.contains("财") { return "banknote.fill" }
        if lower.contains("relationship") || lower.contains("感情") { return "heart.fill" }
        if lower.contains("health") || lower.contains("健康") { return "cross.case.fill" }
        return "sparkle.magnifyingglass"
    }
}
