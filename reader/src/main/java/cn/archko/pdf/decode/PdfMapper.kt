package cn.archko.pdf.decode

import coil3.map.Mapper
import coil3.request.Options

/**
 * @author: archko 2025/5/5 :06:20
 */
class PdfMapper: Mapper<PdfFetcherData, PdfFetcherData> {
    override fun map(
        data: PdfFetcherData,
        options: Options
    ): PdfFetcherData? {
        return data
    }
}