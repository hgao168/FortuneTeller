import SwiftUI
import UIKit

private enum MatchImageSlot {
    case first
    case second
}

struct MatchView: View {
    @State private var matchType: MatchType = .romantic
    @State private var firstImage: UIImage?
    @State private var secondImage: UIImage?
    @State private var personABirth = Date()
    @State private var personBBirth = Date()

    @State private var selectingSlot: MatchImageSlot = .first
    @State private var showCamera = false
    @State private var showPicker = false

    @State private var result: PalmMatchResponse?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    Text("match.title")
                        .font(.title2.weight(.semibold))
                        .multilineTextAlignment(.center)

                    Picker("match.type", selection: $matchType) {
                        ForEach(MatchType.allCases) { mode in
                            Text(mode.localizedKey).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)

                    VStack(spacing: 10) {
                        DatePicker(
                            "match.birth.person1",
                            selection: $personABirth,
                            displayedComponents: [.date, .hourAndMinute]
                        )
                        DatePicker(
                            "match.birth.person2",
                            selection: $personBBirth,
                            displayedComponents: [.date, .hourAndMinute]
                        )
                    }

                    HStack(spacing: 12) {
                        imageCard(image: firstImage, title: "match.person1")
                        imageCard(image: secondImage, title: "match.person2")
                    }

                    VStack(spacing: 10) {
                        Button {
                            selectingSlot = .first
                            showCamera = true
                        } label: {
                            Label("match.capture.person1", systemImage: "camera.fill")
                                .frame(maxWidth: .infinity)
                                .padding()
                        }
                        .background(Color.purple.opacity(0.12))
                        .foregroundColor(.purple)
                        .cornerRadius(12)

                        Button {
                            selectingSlot = .second
                            showCamera = true
                        } label: {
                            Label("match.capture.person2", systemImage: "camera.fill")
                                .frame(maxWidth: .infinity)
                                .padding()
                        }
                        .background(Color.purple.opacity(0.12))
                        .foregroundColor(.purple)
                        .cornerRadius(12)

                        Button {
                            selectingSlot = .first
                            showPicker = true
                        } label: {
                            Label("match.photo.person1", systemImage: "photo.on.rectangle")
                                .frame(maxWidth: .infinity)
                                .padding()
                        }
                        .background(Color(.secondarySystemBackground))
                        .foregroundStyle(.primary)
                        .cornerRadius(12)

                        Button {
                            selectingSlot = .second
                            showPicker = true
                        } label: {
                            Label("match.photo.person2", systemImage: "photo.on.rectangle")
                                .frame(maxWidth: .infinity)
                                .padding()
                        }
                        .background(Color(.secondarySystemBackground))
                        .foregroundStyle(.primary)
                        .cornerRadius(12)
                    }

                    Button {
                        Task { await match() }
                    } label: {
                        if isLoading {
                            ProgressView().tint(.white)
                        } else {
                            Text("match.analyze")
                                .font(.headline)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.purple)
                    .foregroundColor(.white)
                    .cornerRadius(14)
                    .disabled(isLoading || firstImage == nil || secondImage == nil)

                    if let errorMessage {
                        Text(errorMessage)
                            .font(.callout)
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                    }
                }
                .padding()
            }
            .navigationTitle(Text("nav.match"))
            .fullScreenCover(isPresented: $showCamera) {
                PalmCameraView { image in
                    assign(image)
                    showCamera = false
                } onCancel: {
                    showCamera = false
                }
                .ignoresSafeArea()
            }
            .sheet(isPresented: $showPicker) {
                PalmPhotoPicker { image in
                    assign(image)
                    showPicker = false
                } onCancel: {
                    showPicker = false
                }
            }
            .navigationDestination(isPresented: Binding(
                get: { result != nil },
                set: { if !$0 { result = nil } }
            )) {
                if let result {
                    MatchResultView(response: result, imageA: firstImage, imageB: secondImage)
                }
            }
        }
    }

    private func imageCard(image: UIImage?, title: LocalizedStringKey) -> some View {
        VStack(spacing: 8) {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(height: 140)
                    .frame(maxWidth: .infinity)
                    .clipped()
                    .cornerRadius(12)
            } else {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.secondarySystemBackground))
                    .frame(height: 140)
                    .overlay {
                        Image(systemName: "hand.raised")
                            .font(.title)
                            .foregroundStyle(.secondary)
                    }
            }
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    private func assign(_ image: UIImage) {
        switch selectingSlot {
        case .first:
            firstImage = image
        case .second:
            secondImage = image
        }
    }

    private func match() async {
        guard let firstImage, let secondImage else { return }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            result = try await APIClient.shared.matchPalm(
                imageA: firstImage,
                imageB: secondImage,
                matchType: matchType,
                personABirth: personABirth,
                personBBirth: personBBirth
            )
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
