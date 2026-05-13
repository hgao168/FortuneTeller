import XCTest
@testable import FortuneTeller

final class MultipartFormDataTests: XCTestCase {

    func testContentTypeIncludesBoundary() {
        let form = MultipartFormData(boundary: "ABC123")
        XCTAssertEqual(form.contentType, "multipart/form-data; boundary=ABC123")
    }

    func testDefaultBoundaryIsUnique() {
        let a = MultipartFormData()
        let b = MultipartFormData()
        XCTAssertNotEqual(a.boundary, b.boundary)
        XCTAssertTrue(a.boundary.hasPrefix("Boundary-"))
    }

    func testFieldEncoding() {
        var form = MultipartFormData(boundary: "B")
        form.appendField("scope", value: "today")
        let body = String(data: form.finalize(), encoding: .utf8) ?? ""

        let expected =
            "--B\r\n" +
            "Content-Disposition: form-data; name=\"scope\"\r\n" +
            "\r\n" +
            "today\r\n" +
            "--B--\r\n"
        XCTAssertEqual(body, expected)
    }

    func testFileEncodingHeadersAndBytes() {
        var form = MultipartFormData(boundary: "B")
        let payload = Data([0xDE, 0xAD, 0xBE, 0xEF])
        form.appendFile("image", filename: "p.jpg", data: payload)
        let body = form.finalize()

        // Header section must be ASCII-decodable; binary payload sits between
        // the headers and the trailing CRLF.
        let bodyString = String(data: body, encoding: .utf8)
        XCTAssertNotNil(bodyString)
        let s = bodyString ?? ""
        XCTAssertTrue(s.contains("--B\r\n"))
        XCTAssertTrue(s.contains("Content-Disposition: form-data; name=\"image\"; filename=\"p.jpg\"\r\n"))
        XCTAssertTrue(s.contains("Content-Type: image/jpeg\r\n"))
        XCTAssertTrue(s.hasSuffix("--B--\r\n"))

        // Verify the raw payload bytes are present, in order, inside the body.
        XCTAssertTrue(body.range(of: payload) != nil)
    }

    func testMultipleFieldsOrderingPreserved() {
        var form = MultipartFormData(boundary: "B")
        form.appendField("a", value: "1")
        form.appendField("b", value: "2")
        let body = String(data: form.finalize(), encoding: .utf8) ?? ""
        guard
            let aRange = body.range(of: "name=\"a\""),
            let bRange = body.range(of: "name=\"b\"")
        else {
            return XCTFail("Fields missing")
        }
        XCTAssertLessThan(aRange.lowerBound, bRange.lowerBound)
    }

    func testCustomMimeType() {
        var form = MultipartFormData(boundary: "B")
        form.appendFile("file", filename: "f.png", mimeType: "image/png", data: Data([0x01]))
        let body = String(data: form.finalize(), encoding: .utf8) ?? ""
        XCTAssertTrue(body.contains("Content-Type: image/png\r\n"))
    }
}
