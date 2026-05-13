import SwiftUI

struct ContentView: View {
    @StateObject private var store = PalmStore()

    var body: some View {
        TabView {
            ReadTabView()
                .tabItem { Label("nav.read", systemImage: "hand.raised.fill") }
            MatchView()
                .tabItem { Label("nav.match", systemImage: "person.2.fill") }
            HistoryView()
                .tabItem { Label("nav.history", systemImage: "clock.arrow.circlepath") }
        }
        .environmentObject(store)
    }
}

struct ReadTabView: View {
    @EnvironmentObject var store: PalmStore
    @State private var scope: ReadingScope = .today
    @State private var showCamera = false
    @State private var showPicker = false
    @State private var capturedImage: UIImage?
    @State private var reading: PalmReadingResponse?
    @State private var isAnalyzing = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    header

                    scopePicker

                    actionButtons

                    if let image = capturedImage {
                        VStack(spacing: 12) {
                            Image(uiImage: image)
                                .resizable()
                                .scaledToFit()
                                .frame(maxHeight: 260)
                                .cornerRadius(16)

                            Button {
                                Task { await analyze(image: image) }
                            } label: {
                                if isAnalyzing {
                                    ProgressView().tint(.white)
                                } else {
                                    Text("button.analyze")
                                        .font(.headline)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.purple)
                            .foregroundColor(.white)
                            .cornerRadius(14)
                            .disabled(isAnalyzing)
                        }
                    }

                    if let errorMessage {
                        Text(errorMessage)
                            .font(.callout)
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                    }

                    Spacer(minLength: 24)
                }
                .padding()
            }
            .navigationTitle("FortuneTeller")
            .fullScreenCover(isPresented: $showCamera) {
                PalmCameraView { image in
                    capturedImage = image
                    showCamera = false
                } onCancel: {
                    showCamera = false
                }
                .ignoresSafeArea()
            }
            .sheet(isPresented: $showPicker) {
                PalmPhotoPicker { image in
                    capturedImage = image
                    showPicker = false
                } onCancel: {
                    showPicker = false
                }
            }
            .navigationDestination(isPresented: Binding(
                get: { reading != nil },
                set: { if !$0 { reading = nil } }
            )) {
                if let reading {
                    ReadingResultView(response: reading, palmImage: capturedImage)
                }
            }
        }
    }

    private var header: some View {
        VStack(spacing: 8) {
            Text("✋")
                .font(.system(size: 64))
            Text("app.tagline")
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text("app.subtitle")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.top, 8)
    }

    private var scopePicker: some View {
        Picker("reading.scope.label", selection: $scope) {
            ForEach(ReadingScope.allCases) { value in
                Text(value.localizedKey).tag(value)
            }
        }
        .pickerStyle(.menu)
    }

    private var actionButtons: some View {
        VStack(spacing: 12) {
            Button {
                errorMessage = nil
                reading = nil
                showCamera = true
            } label: {
                Label("button.camera", systemImage: "camera.fill")
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .background(Color.purple.opacity(0.12))
            .foregroundColor(.purple)
            .cornerRadius(14)

            Button {
                errorMessage = nil
                reading = nil
                showPicker = true
            } label: {
                Label("button.photo", systemImage: "photo.on.rectangle")
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .background(Color(.secondarySystemBackground))
            .foregroundStyle(.primary)
            .cornerRadius(14)
        }
    }

    private func analyze(image: UIImage) async {
        isAnalyzing = true
        errorMessage = nil
        defer { isAnalyzing = false }
        do {
            let response = try await APIClient.shared.analyzePalm(image: image, scope: scope)
            reading = response
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

#Preview {
    ContentView()
}
