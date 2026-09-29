package com.example

import com.example.engine.FileManager
import com.example.engine.PdfEngine
import com.example.engine.ZipAndUtilityEngine
import com.example.model.PickedFile
import com.example.model.ToolCategory
import com.example.model.ToolId
import com.example.model.ToolRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    @Test
    fun toolRegistry_containsAllRequiredTools() {
        val tools = ToolRegistry.tools
        assertTrue("Registry must have at least 15 tools", tools.size >= 15)

        // Check specific tools requested
        val mergeTool = tools.find { it.id == ToolId.PDF_MERGE }
        assertNotNull("Merge PDF tool must exist", mergeTool)
        assertEquals(ToolCategory.PDF, mergeTool?.category)

        val photoSheetTool = tools.find { it.id == ToolId.PHOTO_SHEET }
        assertNotNull("Photo Sheet Maker tool must exist", photoSheetTool)
        assertEquals(ToolCategory.IMAGE, photoSheetTool?.category)

        val zipTool = tools.find { it.id == ToolId.ZIP_CREATE }
        assertNotNull("ZIP tool must exist", zipTool)
        assertEquals(ToolCategory.UTILITY, zipTool?.category)
    }

    @Test
    fun parsePageRange_parsesCorrectly() {
        val parsed1 = PdfEngine.parsePageRange("1-3, 5", 10)
        assertEquals(listOf(0, 1, 2, 4), parsed1)

        val parsedAll = PdfEngine.parsePageRange("all", 5)
        assertEquals(listOf(0, 1, 2, 3, 4), parsedAll)

        val parsedSingle = PdfEngine.parsePageRange("2", 5)
        assertEquals(listOf(1), parsedSingle)

        val parsedEmpty = PdfEngine.parsePageRange("", 3)
        assertEquals(listOf(0, 1, 2), parsedEmpty)
    }

    @Test
    fun formatFileSize_formatsProperly() {
        assertEquals("0 B", FileManager.formatFileSize(0))
        assertEquals("500 B", FileManager.formatFileSize(500))
        assertTrue(FileManager.formatFileSize(1024).contains("KB"))
        assertTrue(FileManager.formatFileSize(1024 * 1024 * 5).contains("MB"))
    }

    @Test
    fun batchRename_computesPreviewCorrectly() {
        val dummyFiles = listOf(
            PickedFile(name = "photo_one.jpg", sizeBytes = 100, mimeType = "image/jpeg", localFile = File("photo_one.jpg")),
            PickedFile(name = "photo_two.jpg", sizeBytes = 100, mimeType = "image/jpeg", localFile = File("photo_two.jpg"))
        )

        val preview = ZipAndUtilityEngine.computeRenamePreview(
            files = dummyFiles,
            prefix = "Vacation_",
            suffix = "_final",
            useNumbering = true,
            findText = "photo",
            replaceText = "pic"
        )

        assertEquals(2, preview.size)
        assertEquals("photo_one.jpg" to "Vacation_pic_one_001_final.jpg", preview[0])
        assertEquals("photo_two.jpg" to "Vacation_pic_two_002_final.jpg", preview[1])
    }
}
