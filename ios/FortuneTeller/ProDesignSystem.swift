import SwiftUI
import ObjectiveC.runtime

// MARK: - Pro Fun Design System

enum AppLanguage: String, CaseIterable, Identifiable {
    case system
    case english
    case chinese

    var id: String { rawValue }

    var title: LocalizedStringKey {
        switch self {
        case .system: return "language.system"
        case .english: return "language.english"
        case .chinese: return "language.chinese"
        }
    }

    /// Resource code matching an `.lproj` folder.
    var lprojCode: String? {
        switch self {
        case .system: return nil
        case .english: return "en"
        case .chinese: return "zh-Hans"
        }
    }

    /// Locale used for date / number formatting.
    var localeIdentifier: String {
        switch self {
        case .system:  return Locale.current.identifier
        case .english: return "en"
        case .chinese: return "zh-Hans"
        }
    }

    var apiLanguageCode: String {
        switch self {
        case .chinese:
            return "zh-Hans"
        case .english:
            return "en"
        case .system:
            let preferred = Locale.preferredLanguages.first?.lowercased() ?? "en"
            return preferred.hasPrefix("zh") ? "zh-Hans" : "en"
        }
    }
}

// MARK: - Runtime language override
//
// SwiftUI's `\.locale` environment value only affects formatters; it does not
// change which `.lproj` `Text(LocalizedStringKey)` reads from. To make the
// in-app language picker actually swap displayed text, we install a Bundle
// subclass on `Bundle.main` that redirects `localizedString(forKey:...)` to
// the chosen language's lproj bundle.

private final class LanguageOverrideBundle: Bundle, @unchecked Sendable {
    override func localizedString(forKey key: String, value: String?, table tableName: String?) -> String {
        if let code = LanguageRuntime.currentLProj,
           let path = Bundle.main.path(forResource: code, ofType: "lproj"),
           let bundle = Bundle(path: path) {
            return bundle.localizedString(forKey: key, value: value, table: tableName)
        }
        return super.localizedString(forKey: key, value: value, table: tableName)
    }
}

enum LanguageRuntime {
    fileprivate static var currentLProj: String?

    static let installOverride: Void = {
        object_setClass(Bundle.main, LanguageOverrideBundle.self)
    }()

    static func apply(_ language: AppLanguage) {
        _ = installOverride
        currentLProj = language.lprojCode
    }

    static var currentAPILanguageCode: String {
        let raw = UserDefaults.standard.string(forKey: "app.language") ?? AppLanguage.system.rawValue
        let language = AppLanguage(rawValue: raw) ?? .system
        return language.apiLanguageCode
    }

    /// Returns a localized string using the active in-app language.
    static func localized(_ key: String) -> String {
        if let code = currentLProj,
           let path = Bundle.main.path(forResource: code, ofType: "lproj"),
           let bundle = Bundle(path: path) {
            return bundle.localizedString(forKey: key, value: nil, table: nil)
        }
        return Bundle.main.localizedString(forKey: key, value: nil, table: nil)
    }
}

final class AppSettings: ObservableObject {
    @AppStorage("app.language") private var storedLanguage = AppLanguage.system.rawValue
    @Published var language: AppLanguage = .system {
        didSet {
            storedLanguage = language.rawValue
            LanguageRuntime.apply(language)
        }
    }

    init() {
        let lang = AppLanguage(rawValue: storedLanguage) ?? .system
        LanguageRuntime.apply(lang)
        language = lang
    }
}

struct FutureBackground: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 1.00, green: 0.95, blue: 0.86),
                    Color(red: 0.86, green: 0.98, blue: 0.95),
                    Color(red: 0.92, green: 0.90, blue: 1.00)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            Circle()
                .fill(Color(red: 1.00, green: 0.74, blue: 0.56).opacity(0.45))
                .frame(width: 280, height: 280)
                .blur(radius: 58)
                .offset(x: -150, y: -280)

            Circle()
                .fill(Color(red: 0.39, green: 0.87, blue: 0.80).opacity(0.38))
                .frame(width: 260, height: 260)
                .blur(radius: 64)
                .offset(x: 170, y: 170)

            Circle()
                .fill(Color(red: 1.00, green: 0.57, blue: 0.72).opacity(0.34))
                .frame(width: 220, height: 220)
                .blur(radius: 56)
                .offset(x: -120, y: 320)

            RoundedRectangle(cornerRadius: 52, style: .continuous)
                .fill(Color(red: 0.57, green: 0.66, blue: 1.00).opacity(0.26))
                .frame(width: 210, height: 90)
                .rotationEffect(.degrees(24))
                .blur(radius: 26)
                .offset(x: 130, y: -210)
        }
    }
}

struct GlassCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(18)
            .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 28, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .stroke(
                        LinearGradient(
                            colors: [Color.orange.opacity(0.45), Color.pink.opacity(0.28), Color.cyan.opacity(0.35)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
            )
            .shadow(color: Color(red: 0.91, green: 0.48, blue: 0.58).opacity(0.22), radius: 20, x: 0, y: 12)
    }
}

struct ProPrimaryButton: View {
    let title: LocalizedStringKey
    let systemImage: String?
    let isLoading: Bool
    let isDisabled: Bool
    let action: () -> Void

    init(
        _ title: LocalizedStringKey,
        systemImage: String? = nil,
        isLoading: Bool = false,
        isDisabled: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.systemImage = systemImage
        self.isLoading = isLoading
        self.isDisabled = isDisabled
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if isLoading {
                    ProgressView().tint(.white)
                } else {
                    if let systemImage {
                        Image(systemName: systemImage)
                    }
                    Text(title).font(.headline)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .foregroundStyle(Color(red: 0.23, green: 0.16, blue: 0.23))
            .background(
                LinearGradient(
                    colors: isDisabled
                        ? [.gray.opacity(0.45), .gray.opacity(0.25)]
                        : [Color(red: 1.00, green: 0.78, blue: 0.44), Color(red: 1.00, green: 0.57, blue: 0.72), Color(red: 0.54, green: 0.86, blue: 0.95)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                in: RoundedRectangle(cornerRadius: 20, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(.white.opacity(0.72), lineWidth: 1)
            )
        }
        .disabled(isDisabled || isLoading)
    }
}

struct ProSecondaryButton: View {
    let title: LocalizedStringKey
    let systemImage: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 15)
                .foregroundStyle(Color(red: 0.21, green: 0.18, blue: 0.28))
                .background(.white.opacity(0.62), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Color.pink.opacity(0.22), lineWidth: 1)
                )
        }
    }
}

struct LanguageMenu: View {
    @EnvironmentObject private var settings: AppSettings

    var body: some View {
        Menu {
            Picker("language.title", selection: $settings.language) {
                ForEach(AppLanguage.allCases) { language in
                    Text(language.title).tag(language)
                }
            }
        } label: {
            Image(systemName: "sparkles")
                .font(.headline)
                .foregroundStyle(Color(red: 0.28, green: 0.20, blue: 0.39))
                .padding(10)
                .background(.white.opacity(0.74), in: Circle())
                .overlay(Circle().stroke(Color.orange.opacity(0.45), lineWidth: 1))
        }
    }
}

extension View {
    func proScreen() -> some View {
        self
            .background(FutureBackground())
            .scrollContentBackground(.hidden)
    }

    func appLanguageLocale(_ settings: AppSettings) -> some View {
        self.environment(\.locale, Locale(identifier: settings.language.localeIdentifier))
    }
}
