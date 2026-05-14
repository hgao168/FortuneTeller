import SwiftUI

@main
struct FortuneTellerApp: App {
    @StateObject private var settings = AppSettings()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(settings)
                .appLanguageLocale(settings)
                .id(settings.language)
        }
    }
}
