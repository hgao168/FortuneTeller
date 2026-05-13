import SwiftUI

struct ReadingResultView: View {
    let response: PalmReadingResponse
    let palmImage: UIImage?

    @EnvironmentObject var store: PalmStore
    @State private var saved = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if let image = palmImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(maxWidth: .infinity)
                        .frame(height: 220)
                        .clipped()
                        .cornerRadius(16)
                }

                HStack {
                    Text(response.scope.localizedKey)
                        .font(.subheadline.weight(.semibold))
                        .padding(.vertical, 6)
                        .padding(.horizontal, 12)
                        .background(Color.purple.opacity(0.15))
                        .foregroundColor(.purple)
                        .clipShape(Capsule())
                    Spacer()
                }

                Text(response.summary)
                    .font(.title3)
                    .foregroundStyle(.primary)

                ForEach(response.sections) { section in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(section.title)
                            .font(.headline)
                        Text(section.text)
                            .font(.body)
                            .foregroundStyle(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                }

                if !response.advice.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("section.advice")
                            .font(.headline)
                        ForEach(response.advice, id: \.self) { item in
                            HStack(alignment: .top, spacing: 8) {
                                Text("•")
                                Text(item)
                            }
                            .font(.body)
                        }
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                }

                Text(response.disclaimer)
                    .font(.footnote)
                    .foregroundStyle(.tertiary)
                    .multilineTextAlignment(.leading)
            }
            .padding()
        }
        .navigationTitle(Text("nav.reading"))
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    let thumb = palmImage?.jpegData(compressionQuality: 0.5)
                    store.save(SavedReading(palm: response, thumbnailData: thumb))
                    saved = true
                } label: {
                    Label(
                        saved ? String(localized: "button.saved") : String(localized: "button.save"),
                        systemImage: saved ? "checkmark.circle.fill" : "square.and.arrow.down"
                    )
                }
                .disabled(saved)
            }
        }
    }
}
