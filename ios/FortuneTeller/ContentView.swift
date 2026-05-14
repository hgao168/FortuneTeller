import SwiftUI

struct ContentView: View {
    @StateObject private var store = PalmStore()

    var body: some View {
        TabView {
            ReadTabView()
                .tabItem { Label("nav.read", systemImage: "sparkles") }

            MatchView()
                .tabItem { Label("nav.match", systemImage: "heart.text.square.fill") }

            HistoryView()
                .tabItem { Label("nav.history", systemImage: "clock.arrow.circlepath") }
        }
        .tint(Color(red: 0.95, green: 0.46, blue: 0.33))
        .toolbarBackground(.visible, for: .tabBar)
        .toolbarBackground(Color.white.opacity(0.92), for: .tabBar)
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
            ZStack {
                FutureBackground()

                ScrollView {
                    VStack(spacing: 22) {
                        hero
                        scopeCard
                        captureCard
                        previewCard

                        if let errorMessage {
                            Text(errorMessage)
                                .font(.callout)
                                .foregroundStyle(.red.opacity(0.95))
                                .multilineTextAlignment(.center)
                                .padding()
                                .background(.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 18))
                        }

                        Spacer(minLength: 28)
                    }
                    .padding(20)
                }
            }
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    LanguageMenu()
                }
            }
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

    private var hero: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(.white.opacity(0.62))
                    .frame(width: 118, height: 118)
                    .overlay(Circle().stroke(Color.orange.opacity(0.32), lineWidth: 1))

                Image(systemName: "hand.wave.fill")
                    .font(.system(size: 54, weight: .semibold))
                    .foregroundStyle(
                        LinearGradient(colors: [Color.orange, Color.pink.opacity(0.9), Color.mint.opacity(0.9)],
                                       startPoint: .topLeading,
                                       endPoint: .bottomTrailing)
                    )
                    .shadow(color: Color.orange.opacity(0.4), radius: 18)
            }

            VStack(spacing: 8) {
                Text("app.hero.title")
                    .font(.system(size: 34, weight: .heavy, design: .rounded))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)

                Text("app.subtitle")
                    .font(.callout)
                    .foregroundStyle(.white.opacity(0.80))
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
            }
        }
        .padding(.top, 16)
    }

    private var scopeCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 14) {
                Label("reading.scope.label", systemImage: "wand.and.stars")
                    .font(.headline)
                    .foregroundStyle(.white)

                Picker("reading.scope.label", selection: $scope) {
                    ForEach(ReadingScope.allCases) { value in
                        Text(value.localizedKey).tag(value)
                    }
                }
                .pickerStyle(.segmented)
            }
        }
    }

    private var captureCard: some View {
        GlassCard {
            VStack(spacing: 12) {
                Button {
                    errorMessage = nil
                    reading = nil
                    capturedImage = nil
                    showPicker = false
                    showCamera = true
                } label: {
                    Label("button.camera", systemImage: "camera.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 15)
                        .foregroundStyle(.white)
                        .background(.white.opacity(0.14), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                .stroke(.white.opacity(0.25), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)

                Button {
                    errorMessage = nil
                    reading = nil
                    capturedImage = nil
                    showCamera = false
                    showPicker = true
                } label: {
                    Label("button.photo", systemImage: "photo.on.rectangle.angled")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 15)
                        .foregroundStyle(.white)
                        .background(.white.opacity(0.14), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                .stroke(.white.opacity(0.25), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private var previewCard: some View {
        if let image = capturedImage {
            GlassCard {
                VStack(spacing: 14) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(height: 285)
                        .frame(maxWidth: .infinity)
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .stroke(.white.opacity(0.20), lineWidth: 1)
                        )

                    ProPrimaryButton(
                        "button.analyze",
                        systemImage: "sparkles",
                        isLoading: isAnalyzing,
                        isDisabled: isAnalyzing
                    ) {
                        Task { await analyze(image: image) }
                    }
                }
            }
        } else {
            GlassCard {
                VStack(spacing: 10) {
                    Image(systemName: "viewfinder.circle.fill")
                        .font(.system(size: 44))
                        .foregroundStyle(Color(red: 0.92, green: 0.58, blue: 0.33))
                    Text("app.empty.preview.title")
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text("app.empty.preview.subtitle")
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.76))
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
            }
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
        .environmentObject(AppSettings())
}
