package com.lelysnails.agenda.ui

import android.graphics.Color
import com.lelysnails.agenda.data.AppStyle

data class StylePalette(
    val primary: Int, val primaryDark: Int, val primaryLight: Int, val onPrimary: Int,
    val ink: Int, val muted: Int, val bg: Int, val surface: Int, val line: Int,
    val freeBg: Int, val freeStroke: Int,
    val partialStart: Int, val partialEnd: Int, val partialInk: Int,
    val fullStart: Int, val fullEnd: Int,
    val offBg: Int, val offStroke: Int, val offText: Int,
    val headerStart: Int, val headerEnd: Int,
    val cornerCard: Float, val cornerDay: Float, val cornerBtn: Float,
    val logoEmoji: String, val isDark: Boolean
) {
    companion object {
        fun resolve(style: AppStyle, dark: Boolean): StylePalette = when (style) {
            AppStyle.ROSA -> if (dark) p(
                "#F095B0","#E8799A","#F7A8BD","#1A1014","#F5E6EB","#B89AA5","#1A1216","#2A1E24","#3D2B33",
                "#2A1E24","#5A3D48","#5C4A10","#C9A227","#FFE9A0","#8B2E36","#E63946",
                "#222830","#4A5560","#8A96A3","#8B4A5E","#5C2A3D",26f,14f,13f,"\uD83D\uDC85",true
            ) else p(
                "#E8799A","#D4648A","#F7A8BD","#FFFFFF","#3D2B33","#A89099","#FDF6F9","#FFFFFF","#F2E2E9",
                "#FFFFFF","#E8C4D0","#FFF3B0","#FFD93D","#7A5D00","#FF8A8A","#E63946",
                "#E4E9EF","#9AA8B8","#7A8A9A","#F9B4C7","#CF5F86",26f,14f,13f,"\uD83D\uDC85",false
            )
            AppStyle.PRO -> if (dark) p(
                "#5B8DEF","#3D5A80","#7BA3F0","#0D1520","#E8EEF5","#8A9BB0","#0D1520","#162033","#243044",
                "#162033","#2E4060","#1A3050","#3D6BBF","#C5D8FF","#6B4020","#C47A3A",
                "#141A22","#3A4555","#6B7785","#1E3A5F","#0D1520",12f,8f,8f,"\uD83D\uDCBC",true
            ) else p(
                "#1E3A5F","#152A45","#3D5A80","#FFFFFF","#1B2838","#6B7C8F","#F0F3F7","#FFFFFF","#D5DEE8",
                "#FFFFFF","#B8C5D4","#E8F0FE","#5B8DEF","#1A3A6B","#F5C6A0","#C47A3A",
                "#E8ECF0","#8A96A3","#6B7785","#2C4A6E","#1E3A5F",12f,8f,8f,"\uD83D\uDCBC",false
            )
            AppStyle.MINIMAL -> if (dark) p(
                "#F5F5F5","#FFFFFF","#DDDDDD","#000000","#F5F5F5","#999999","#0A0A0A","#161616","#2A2A2A",
                "#161616","#3A3A3A","#2A2A2A","#555555","#EEEEEE","#888888","#F5F5F5",
                "#121212","#333333","#666666","#1A1A1A","#000000",4f,2f,2f,"\u25FB",true
            ) else p(
                "#111111","#000000","#333333","#FFFFFF","#111111","#888888","#FAFAFA","#FFFFFF","#E5E5E5",
                "#FFFFFF","#CCCCCC","#F0F0F0","#AAAAAA","#333333","#444444","#111111",
                "#EEEEEE","#BBBBBB","#999999","#222222","#000000",4f,2f,2f,"\u25FB",false
            )
            AppStyle.LAVANDA -> if (dark) p(
                "#C4B0DC","#9B7EBD","#D9CBF0","#1A1224","#F0E8F8","#A894B8","#16101F","#241A30","#3A2D48",
                "#241A30","#4A3A60","#3A2A55","#7B5EA7","#E8D8FF","#6B2A2A","#C53030",
                "#1C1625","#4A3D58","#8A7A9A","#5A4080","#2E1F45",28f,16f,16f,"\uD83C\uDF3A",true
            ) else p(
                "#9B7EBD","#7B5EA7","#C4B0DC","#FFFFFF","#2E2438","#8F8199","#F8F4FC","#FFFFFF","#E8DFF0",
                "#FFFFFF","#D4C4E8","#EDE4FF","#B794F4","#4A2D7A","#F6ADAD","#C53030",
                "#EDE8F2","#A89BB5","#7D708F","#C4B0DC","#7B5EA7",28f,16f,16f,"\uD83C\uDF3A",false
            )
            AppStyle.OCEANO -> if (dark) p(
                "#2DD4BF","#0D9488","#5EEAD4","#042F2E","#E6FFFA","#7AB8B0","#042F2E","#0A3D3A","#134E4A",
                "#0A3D3A","#1A5C56","#5C3D0A","#D97706","#FEF3C7","#7F1D1D","#DC2626",
                "#0F2A28","#2A4A48","#6B908A","#0D9488","#042F2E",20f,12f,12f,"\uD83C\uDF0A",true
            ) else p(
                "#0D9488","#0F766E","#5EEAD4","#FFFFFF","#134E4A","#5F8A85","#F0FDFA","#FFFFFF","#CCFBF1",
                "#FFFFFF","#99F6E4","#FEF3C7","#F59E0B","#78350F","#FCA5A5","#DC2626",
                "#E2E8F0","#94A3B8","#64748B","#2DD4BF","#0F766E",20f,12f,12f,"\uD83C\uDF0A",false
            )
            AppStyle.DORADO -> if (dark) p(
                "#DAA520","#B8860B","#F0D78C","#1A1508","#F5ECD0","#B0A070","#1A1508","#2A2210","#3D3420",
                "#2A2210","#4A3E20","#4A3A10","#B8860B","#FFE9A0","#6B2020","#C04040",
                "#1E1A10","#4A4030","#8A7E60","#8B6914","#1A1508",18f,10f,10f,"\u2728",true
            ) else p(
                "#B8860B","#8B6914","#DAA520","#FFFFFF","#3D2E0A","#8A7A50","#FFFBF0","#FFFEF7","#F0E6C8",
                "#FFFEF7","#E8D9A8","#FFF0D0","#E8B84A","#5C4200","#E8A0A0","#A03030",
                "#F0EBE0","#B0A480","#8A7E60","#DAA520","#8B6914",18f,10f,10f,"\u2728",false
            )
        }

        private fun p(
            primary: String, primaryDark: String, primaryLight: String, onPrimary: String,
            ink: String, muted: String, bg: String, surface: String, line: String,
            freeBg: String, freeStroke: String,
            partialStart: String, partialEnd: String, partialInk: String,
            fullStart: String, fullEnd: String,
            offBg: String, offStroke: String, offText: String,
            headerStart: String, headerEnd: String,
            cornerCard: Float, cornerDay: Float, cornerBtn: Float,
            logoEmoji: String, isDark: Boolean
        ) = StylePalette(
            c(primary), c(primaryDark), c(primaryLight), c(onPrimary),
            c(ink), c(muted), c(bg), c(surface), c(line),
            c(freeBg), c(freeStroke),
            c(partialStart), c(partialEnd), c(partialInk),
            c(fullStart), c(fullEnd),
            c(offBg), c(offStroke), c(offText),
            c(headerStart), c(headerEnd),
            cornerCard, cornerDay, cornerBtn, logoEmoji, isDark
        )

        private fun c(hex: String) = Color.parseColor(hex)
    }
}
