import SwiftUI

struct MatchResultView: View {
    let response: PalmMatchResponse
    var imageA: UIImage? = nil
    var imageB: UIImage? = nil

    @EnvironmentObject var store: PalmStore
    @State private var saved = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Text(response.matchType.localizedKey)
                        .font(.subheadline.weight(.semibold))
                        .padding(.vertical, 6)
                        .padding(.horizontal, 12)
                        .background(matchTypeColor.opacity(0.15))
                        .foregroundColor(matchTypeColor)
                        .clipShape(Capsule())
                    Spacer()
                    Text("\(response.score)%")
                        .font(.title2.weight(.bold))
                        .foregroundColor(.purple)
                }

                Text(response.summary)
                    .font(.title3)
                    .foregroundStyle(.primary)

                VStack(alignment: .leading, spacing: 8) {
                    Text("match.strengths")
                        .font(.headline)
                    ForEach(response.strengths, id: \.self) { item in
                        HStack(alignment: .top, spacing: 8) {
                            Text("•")
                            Text(item)
                        }
                    }
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 8) {
                    Text("match.tensions")
                        .font(.headline)
                    ForEach(response.tensions, id: \.self) { item in
                        HStack(alignment: .top, spacing: 8) {
                            Text("•")
                            Text(item)
                        }
                    }
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 8) {
                    Text("match.advice")
                        .font(.headline)
                    ForEach(response.advice, id: \.self) { item in
                        HStack(alignment: .top, spacing: 8) {
                            Text("•")
                            Text(item)
                        }
                    }
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(12)

                Text(response.disclaimer)
                    .font(.footnote)
                    .foregroundStyle(.tertiary)
            }
            .padding()
        }
        .navigationTitle(Text("nav.match.result"))
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    let thumbA = imageA?.jpegData(compressionQuality: 0.5)
                    let thumbB = imageB?.jpegData(compressionQuality: 0.5)
                    store.save(SavedReading(match: response, thumbnailA: thumbA, thumbnailB: thumbB))
                    saved = true
                } label: {
                    Label(
                        saved ? LanguageRuntime.localized("button.saved") : LanguageRuntime.localized("button.save"),
                        systemImage: saved ? "checkmark.circle.fill" : "square.and.arrow.down"
                    )
                }
                .disabled(saved)
            }
        }
    }

    private var matchTypeColor: Color {
        switch response.matchType {
        case .romantic:
            return .pink
        case .friend:
            return .blue
        }
    }
}
