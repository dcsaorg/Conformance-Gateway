import {
  ConformanceStatus,
  getConformanceStatusEmoji,
  getConformanceStatusTitle,
} from "./conformance-status";

describe("conformance status presentation", () => {
  it.each([
    [ConformanceStatus.CONFORMANT, "✅", "Conformant"],
    [ConformanceStatus.NON_CONFORMANT, "🚫", "Non-conformant"],
    [ConformanceStatus.COMPLETED_WITHOUT_TRAFFIC, "✔️", "Completed without optional traffic"],
    [ConformanceStatus.SKIPPED, "↪️", "Skipped"],
    [ConformanceStatus.NO_TRAFFIC, "❔", "No traffic"],
    [ConformanceStatus.IRRELEVANT, "➖", "Irrelevant"],
  ])("renders %s as %s", (status, emoji, title) => {
    expect(getConformanceStatusEmoji(status)).toBe(emoji);
    expect(getConformanceStatusTitle(status)).toBe(title);
  });
});

