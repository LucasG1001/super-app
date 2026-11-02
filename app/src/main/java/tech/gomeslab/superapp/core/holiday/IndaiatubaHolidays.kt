package tech.gomeslab.superapp.core.holiday

import tech.gomeslab.superapp.core.holiday.HolidayRule.EasterOffset
import tech.gomeslab.superapp.core.holiday.HolidayRule.Fixed
import tech.gomeslab.superapp.core.holiday.HolidayType.MUNICIPAL
import tech.gomeslab.superapp.core.holiday.HolidayType.NATIONAL
import tech.gomeslab.superapp.core.holiday.HolidayType.OPTIONAL
import tech.gomeslab.superapp.core.holiday.HolidayType.STATE

// Municipais conforme o edital anual do TJSP/CSM ("INDAIATUBA – 02/02, Sexta-Feira Santa e Corpus
// Christi", DJE/SP 25/11/2025). A prefeitura pode alterar datas: revisar o edital a cada ano.
object IndaiatubaHolidays {

    const val CITY = "Indaiatuba – SP"

    val rules: List<HolidayRule> = listOf(
        Fixed(1, 1, "Confraternização Universal", NATIONAL),
        Fixed(4, 21, "Tiradentes", NATIONAL),
        Fixed(5, 1, "Dia do Trabalho", NATIONAL),
        Fixed(9, 7, "Independência do Brasil", NATIONAL),
        Fixed(10, 12, "Nossa Senhora Aparecida", NATIONAL),
        Fixed(11, 2, "Finados", NATIONAL),
        Fixed(11, 15, "Proclamação da República", NATIONAL),
        Fixed(11, 20, "Dia da Consciência Negra", NATIONAL),
        Fixed(12, 25, "Natal", NATIONAL),

        Fixed(7, 9, "Revolução Constitucionalista", STATE),

        Fixed(2, 2, "Nossa Senhora da Candelária", MUNICIPAL, "Padroeira de Indaiatuba"),
        EasterOffset(-2, "Sexta-feira Santa", MUNICIPAL),
        EasterOffset(60, "Corpus Christi", MUNICIPAL),

        EasterOffset(-48, "Carnaval", OPTIONAL),
        EasterOffset(-47, "Carnaval", OPTIONAL),
        EasterOffset(-46, "Quarta-feira de Cinzas", OPTIONAL, "Até as 14h"),
        Fixed(10, 28, "Dia do Servidor Público", OPTIONAL),
        Fixed(12, 24, "Véspera de Natal", OPTIONAL, "Após as 14h"),
        Fixed(12, 31, "Véspera de Ano-Novo", OPTIONAL, "Após as 14h"),
    )
}
