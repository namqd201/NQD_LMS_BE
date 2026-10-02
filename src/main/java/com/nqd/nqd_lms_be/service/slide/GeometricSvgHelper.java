package com.nqd.nqd_lms_be.service.slide;

import org.apache.poi.sl.usermodel.ShapeType;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class GeometricSvgHelper {

    private GeometricSvgHelper() {}

    private static String normalize(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase(Locale.ROOT);
        String nfd = Normalizer.normalize(lower, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace('đ', 'd').replace('Đ', 'd');
    }

    /**
     * Detects if the slide topic relates to geometry and returns a clean, textbook-grade SVG diagram.
     * Returns null if no geometric topic matches.
     */
    public static String detectAndGenerateSvg(String title, String subtitle, String content) {
        String combined = normalize((title != null ? title : "") + " " + (subtitle != null ? subtitle : "") + " " + (content != null ? content : ""));

        // 1. Hình vuông (Square)
        if (combined.contains("hinh vuong") || combined.contains("vuong vuc")) {
            return generateSquareSvg();
        }

        // 2. Hình chữ nhật (Rectangle)
        if (combined.contains("hinh chu nhat") || combined.contains("chu nhat")) {
            return generateRectangleSvg();
        }

        // 3. Hình tròn (Circle)
        if (combined.contains("hinh tron") || combined.contains("duong tron") || combined.contains("hinh cau")) {
            return generateCircleSvg();
        }

        // 4. Tam giác vuông (Right triangle)
        if (combined.contains("tam giac vuong")) {
            return generateRightTriangleSvg();
        }

        // 5. Tam giác đều / Tam giác cân / Tam giác chung
        if (combined.contains("tam giac")) {
            return generateTriangleSvg();
        }

        // 6. Hình thoi (Rhombus)
        if (combined.contains("hinh thoi")) {
            return generateRhombusSvg();
        }

        // 7. Hình thang (Trapezoid)
        if (combined.contains("hinh thang")) {
            return generateTrapezoidSvg();
        }

        // 8. Hình bình hành (Parallelogram)
        if (combined.contains("hinh binh hanh")) {
            return generateParallelogramSvg();
        }

        // 9. Hình hộp chữ nhật / Hình lập phương (3D Cube / Cuboid)
        if (combined.contains("lap phuong") || combined.contains("hop chu nhat")) {
            return generateCubeSvg();
        }

        return null;
    }

    /**
     * Maps detected geometric concepts to Apache POI ShapeType for native PowerPoint drawing.
     */
    public static ShapeType detectPoiShapeType(String title, String subtitle, String content) {
        String combined = normalize((title != null ? title : "") + " " + (subtitle != null ? subtitle : "") + " " + (content != null ? content : ""));
        if (combined.contains("hinh vuong") || combined.contains("vuong vuc")) {
            return ShapeType.RECT;
        }
        if (combined.contains("hinh chu nhat") || combined.contains("chu nhat")) {
            return ShapeType.ROUND_RECT;
        }
        if (combined.contains("hinh tron") || combined.contains("duong tron") || combined.contains("hinh cau")) {
            return ShapeType.ELLIPSE;
        }
        if (combined.contains("tam giac")) {
            return ShapeType.TRIANGLE;
        }
        if (combined.contains("hinh thoi")) {
            return ShapeType.DIAMOND;
        }
        if (combined.contains("hinh thang")) {
            return ShapeType.TRAPEZOID;
        }
        if (combined.contains("hinh binh hanh")) {
            return ShapeType.PARALLELOGRAM;
        }
        if (combined.contains("lap phuong") || combined.contains("hop chu nhat")) {
            return ShapeType.CUBE;
        }
        return null;
    }

    public static String generateSquareSvg() {
        return """
            <svg viewBox="0 0 280 210" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
              <defs>
                <linearGradient id="sqG" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#83C75D" stop-opacity="0.2"/>
                  <stop offset="100%" stop-color="#6366F1" stop-opacity="0.25"/>
                </linearGradient>
              </defs>
              <rect x="70" y="35" width="140" height="140" rx="6" fill="url(#sqG)" stroke="#4F46E5" stroke-width="3"/>
              <!-- 4 right angles -->
              <path d="M 70 51 L 86 51 L 86 35" fill="none" stroke="#E11D48" stroke-width="2"/>
              <path d="M 194 35 L 194 51 L 210 51" fill="none" stroke="#E11D48" stroke-width="2"/>
              <path d="M 194 175 L 194 159 L 210 159" fill="none" stroke="#E11D48" stroke-width="2"/>
              <path d="M 70 159 L 86 159 L 86 175" fill="none" stroke="#E11D48" stroke-width="2"/>
              <!-- Equal tick marks -->
              <line x1="140" y1="30" x2="140" y2="40" stroke="#0D9488" stroke-width="3"/>
              <line x1="205" y1="105" x2="215" y2="105" stroke="#0D9488" stroke-width="3"/>
              <line x1="140" y1="170" x2="140" y2="180" stroke="#0D9488" stroke-width="3"/>
              <line x1="65" y1="105" x2="75" y2="105" stroke="#0D9488" stroke-width="3"/>
              <!-- Vertices -->
              <text x="52" y="32" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">A</text>
              <text x="216" y="32" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">B</text>
              <text x="216" y="190" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">C</text>
              <text x="52" y="190" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">D</text>
              <!-- Label -->
              <text x="140" y="24" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="12" fill="#4338CA">Cạnh a</text>
              <text x="140" y="112" text-anchor="middle" font-family="sans-serif" font-weight="900" font-size="13" fill="#1E293B">Hình vuông ABCD</text>
            </svg>
            """;
    }

    public static String generateRectangleSvg() {
        return """
            <svg viewBox="0 0 280 210" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
              <defs>
                <linearGradient id="rectG" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#38BDF8" stop-opacity="0.2"/>
                  <stop offset="100%" stop-color="#6366F1" stop-opacity="0.25"/>
                </linearGradient>
              </defs>
              <rect x="45" y="55" width="190" height="105" rx="6" fill="url(#rectG)" stroke="#0284C7" stroke-width="3"/>
              <!-- 4 right angles -->
              <path d="M 45 70 L 60 70 L 60 55" fill="none" stroke="#E11D48" stroke-width="2"/>
              <path d="M 220 55 L 220 70 L 235 70" fill="none" stroke="#E11D48" stroke-width="2"/>
              <path d="M 220 160 L 220 145 L 235 145" fill="none" stroke="#E11D48" stroke-width="2"/>
              <path d="M 45 145 L 60 145 L 60 160" fill="none" stroke="#E11D48" stroke-width="2"/>
              <!-- Vertices -->
              <text x="32" y="50" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">A</text>
              <text x="240" y="50" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">B</text>
              <text x="240" y="175" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">C</text>
              <text x="32" y="175" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">D</text>
              <!-- Dimensions -->
              <text x="140" y="44" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="12" fill="#0369A1">Chiều dài a</text>
              <text x="250" y="112" font-family="sans-serif" font-weight="bold" font-size="12" fill="#0369A1">Rộng b</text>
              <text x="140" y="112" text-anchor="middle" font-family="sans-serif" font-weight="900" font-size="13" fill="#0F172A">Hình chữ nhật ABCD</text>
            </svg>
            """;
    }

    public static String generateCircleSvg() {
        return """
            <svg viewBox="0 0 280 210" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
              <defs>
                <radialGradient id="circG" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#FDE047" stop-opacity="0.3"/>
                  <stop offset="100%" stop-color="#E11D48" stop-opacity="0.18"/>
                </radialGradient>
              </defs>
              <!-- Circle Body -->
              <circle cx="140" cy="105" r="70" fill="url(#circG)" stroke="#E11D48" stroke-width="3"/>
              <!-- Center point O -->
              <circle cx="140" cy="105" r="4.5" fill="#E11D48"/>
              <text x="142" y="98" font-family="sans-serif" font-weight="900" font-size="14" fill="#9F1239">O</text>
              <!-- Radius Line -->
              <line x1="140" y1="105" x2="210" y2="105" stroke="#E11D48" stroke-width="2.5" stroke-dasharray="4 3"/>
              <circle cx="210" cy="105" r="3.5" fill="#E11D48"/>
              <text x="216" y="110" font-family="sans-serif" font-weight="900" font-size="13" fill="#0F172A">A</text>
              <text x="175" y="100" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="13" fill="#BE123C">Bán kính R</text>
              <!-- Diameter line dashed -->
              <line x1="70" y1="105" x2="140" y2="105" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="3 3"/>
              <text x="140" y="195" text-anchor="middle" font-family="sans-serif" font-weight="900" font-size="13" fill="#1E293B">Hình tròn tâm O, bán kính R</text>
            </svg>
            """;
    }

    public static String generateTriangleSvg() {
        return """
            <svg viewBox="0 0 280 210" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
              <defs>
                <linearGradient id="triG" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#10B981" stop-opacity="0.25"/>
                  <stop offset="100%" stop-color="#3B82F6" stop-opacity="0.2"/>
                </linearGradient>
              </defs>
              <polygon points="140,35 60,165 220,165" fill="url(#triG)" stroke="#059669" stroke-width="3"/>
              <!-- Height Line AH -->
              <line x1="140" y1="35" x2="140" y2="165" stroke="#E11D48" stroke-width="2" stroke-dasharray="4 3"/>
              <!-- Right angle at H -->
              <path d="M 140 152 L 153 152 L 153 165" fill="none" stroke="#E11D48" stroke-width="1.5"/>
              <!-- Vertices -->
              <text x="135" y="26" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">A</text>
              <text x="45" y="175" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">B</text>
              <text x="226" y="175" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">C</text>
              <text x="135" y="180" font-family="sans-serif" font-weight="bold" font-size="12" fill="#E11D48">H</text>
              <text x="146" y="105" font-family="sans-serif" font-weight="bold" font-size="12" fill="#E11D48">h</text>
              <text x="140" y="200" text-anchor="middle" font-family="sans-serif" font-weight="900" font-size="13" fill="#065F46">Tam giác ABC (Đáy BC, Chiều cao AH)</text>
            </svg>
            """;
    }

    public static String generateRightTriangleSvg() {
        return """
            <svg viewBox="0 0 280 210" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
              <defs>
                <linearGradient id="rtG" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#8B5CF6" stop-opacity="0.2"/>
                  <stop offset="100%" stop-color="#EC4899" stop-opacity="0.25"/>
                </linearGradient>
              </defs>
              <polygon points="65,45 65,165 225,165" fill="url(#rtG)" stroke="#7C3AED" stroke-width="3"/>
              <!-- Right angle at A -->
              <path d="M 65 148 L 82 148 L 82 165" fill="none" stroke="#E11D48" stroke-width="2"/>
              <!-- Vertices -->
              <text x="50" y="42" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">B</text>
              <text x="48" y="178" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">A</text>
              <text x="232" y="172" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">C</text>
              <!-- Hypotenuse label -->
              <text x="155" y="98" font-family="sans-serif" font-weight="bold" font-size="12" fill="#6D28D9">Cạnh huyền BC</text>
              <text x="140" y="195" text-anchor="middle" font-family="sans-serif" font-weight="900" font-size="13" fill="#5B21B6">Tam giác vuông tại A</text>
            </svg>
            """;
    }

    public static String generateRhombusSvg() {
        return """
            <svg viewBox="0 0 280 210" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
              <defs>
                <linearGradient id="rhG" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#F59E0B" stop-opacity="0.25"/>
                  <stop offset="100%" stop-color="#10B981" stop-opacity="0.25"/>
                </linearGradient>
              </defs>
              <polygon points="140,30 225,105 140,180 55,105" fill="url(#rhG)" stroke="#D97706" stroke-width="3"/>
              <!-- Diagonals -->
              <line x1="140" y1="30" x2="140" y2="180" stroke="#0D9488" stroke-width="1.8" stroke-dasharray="4 3"/>
              <line x1="55" y1="105" x2="225" y2="105" stroke="#0D9488" stroke-width="1.8" stroke-dasharray="4 3"/>
              <!-- Center right angle -->
              <path d="M 140 95 L 150 95 L 150 105" fill="none" stroke="#E11D48" stroke-width="1.5"/>
              <!-- Vertices -->
              <text x="135" y="24" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">A</text>
              <text x="232" y="110" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">B</text>
              <text x="135" y="196" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">C</text>
              <text x="40" y="110" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">D</text>
              <text x="140" y="115" text-anchor="middle" font-family="sans-serif" font-weight="900" font-size="13" fill="#92400E">Hình thoi ABCD (2 đường chéo vuông góc)</text>
            </svg>
            """;
    }

    public static String generateTrapezoidSvg() {
        return """
            <svg viewBox="0 0 280 210" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
              <defs>
                <linearGradient id="tzG" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#6366F1" stop-opacity="0.2"/>
                  <stop offset="100%" stop-color="#06B6D4" stop-opacity="0.25"/>
                </linearGradient>
              </defs>
              <polygon points="90,45 190,45 235,160 45,160" fill="url(#tzG)" stroke="#4338CA" stroke-width="3"/>
              <!-- Height AH -->
              <line x1="90" y1="45" x2="90" y2="160" stroke="#E11D48" stroke-width="2" stroke-dasharray="4 3"/>
              <path d="M 90 148 L 102 148 L 102 160" fill="none" stroke="#E11D48" stroke-width="1.5"/>
              <text x="94" y="105" font-family="sans-serif" font-weight="bold" font-size="12" fill="#E11D48">h</text>
              <!-- Vertices -->
              <text x="78" y="38" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">A</text>
              <text x="195" y="38" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">B</text>
              <text x="242" y="168" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">C</text>
              <text x="32" y="168" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">D</text>
              <text x="140" y="36" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="12" fill="#4338CA">Đáy bé a</text>
              <text x="140" y="178" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="12" fill="#4338CA">Đáy lớn b</text>
              <text x="140" y="200" text-anchor="middle" font-family="sans-serif" font-weight="900" font-size="13" fill="#312E81">Hình thang ABCD</text>
            </svg>
            """;
    }

    public static String generateParallelogramSvg() {
        return """
            <svg viewBox="0 0 280 210" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
              <defs>
                <linearGradient id="plG" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#14B8A6" stop-opacity="0.2"/>
                  <stop offset="100%" stop-color="#3B82F6" stop-opacity="0.25"/>
                </linearGradient>
              </defs>
              <polygon points="85,45 235,45 195,160 45,160" fill="url(#plG)" stroke="#0F766E" stroke-width="3"/>
              <!-- Height -->
              <line x1="85" y1="45" x2="85" y2="160" stroke="#E11D48" stroke-width="2" stroke-dasharray="4 3"/>
              <path d="M 85 148 L 97 148 L 97 160" fill="none" stroke="#E11D48" stroke-width="1.5"/>
              <!-- Vertices -->
              <text x="75" y="38" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">A</text>
              <text x="240" y="38" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">B</text>
              <text x="202" y="170" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">C</text>
              <text x="32" y="170" font-family="sans-serif" font-weight="900" font-size="14" fill="#0F172A">D</text>
              <text x="140" y="196" text-anchor="middle" font-family="sans-serif" font-weight="900" font-size="13" fill="#134E4A">Hình bình hành ABCD</text>
            </svg>
            """;
    }

    public static String generateCubeSvg() {
        return """
            <svg viewBox="0 0 280 210" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
              <defs>
                <linearGradient id="cubeG1" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#83C75D" stop-opacity="0.3"/>
                  <stop offset="100%" stop-color="#6366F1" stop-opacity="0.3"/>
                </linearGradient>
              </defs>
              <!-- Front Face -->
              <rect x="65" y="80" width="100" height="95" fill="url(#cubeG1)" stroke="#4338CA" stroke-width="2.5"/>
              <!-- Top Face -->
              <polygon points="65,80 115,40 215,40 165,80" fill="#6366F1" fill-opacity="0.18" stroke="#4338CA" stroke-width="2.5"/>
              <!-- Right Face -->
              <polygon points="165,80 215,40 215,135 165,175" fill="#4338CA" fill-opacity="0.25" stroke="#4338CA" stroke-width="2.5"/>
              <!-- Hidden edges dashed -->
              <line x1="65" y1="175" x2="115" y2="135" stroke="#94A3B8" stroke-width="1.8" stroke-dasharray="4 3"/>
              <line x1="115" y1="135" x2="215" y2="135" stroke="#94A3B8" stroke-width="1.8" stroke-dasharray="4 3"/>
              <line x1="115" y1="40" x2="115" y2="135" stroke="#94A3B8" stroke-width="1.8" stroke-dasharray="4 3"/>
              <text x="140" y="200" text-anchor="middle" font-family="sans-serif" font-weight="900" font-size="13" fill="#1E293B">Hình hộp không gian 3D</text>
            </svg>
            """;
    }
}
