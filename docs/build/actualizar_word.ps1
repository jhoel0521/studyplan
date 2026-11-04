# Actualiza índice, lista de tablas y lista de figuras del documento y exporta a PDF.
# Requiere Microsoft Word instalado. Uso: powershell -ExecutionPolicy Bypass -File docs/build/actualizar_word.ps1
$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$docx = Join-Path $root "docs\entrega1\StudyPlan_Entrega1.docx"
$pdf  = Join-Path $root "docs\entrega1\StudyPlan_Entrega1.pdf"

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $doc = $word.Documents.Open($docx, $false, $false, $false)
    $doc.Fields.Update() | Out-Null            # numeración SEQ de tablas y figuras
    foreach ($t in $doc.TablesOfContents) { $t.Update() }
    foreach ($t in $doc.TablesOfFigures)  { $t.Update() }
    $doc.Fields.Update() | Out-Null            # números de página finales
    foreach ($t in $doc.TablesOfContents) { $t.UpdatePageNumbers() }
    foreach ($t in $doc.TablesOfFigures)  { $t.UpdatePageNumbers() }
    $doc.Save()
    $doc.ExportAsFixedFormat($pdf, 17)         # 17 = wdExportFormatPDF
    "Paginas: " + $doc.ComputeStatistics(2)
    $doc.Close()
} finally {
    $word.Quit()
}
"OK $docx"
"OK $pdf"
