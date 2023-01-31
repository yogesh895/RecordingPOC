//import java.io.FileInputStream
//import org.jgltf.impl.Gltf
//import org.jgltf.io.glb.GltfGlbReader
//
//fun mergeGlbs(file1: String, file2: String): Gltf {
//    // Read the first GLB file
//    val gltf1 = GltfGlbReader().read(FileInputStream(file1))
//
//    // Read the second GLB file
//    val gltf2 = GltfGlbReader().read(FileInputStream(file2))
//
//    // Merge the scenes of the two GLB files
//    gltf1.scenes.addAll(gltf2.scenes)
//
//    // Merge the nodes of the two GLB files
//    gltf1.nodes.addAll(gltf2.nodes)
//
//    // Merge the meshes of the two GLB files
//    gltf1.meshes.addAll(gltf2.meshes)
//
//    // Merge the materials of the two GLB files
//    gltf1.materials.addAll(gltf2.materials)
//
//    // Merge the textures of the two GLB files
//    gltf1.textures.addAll(gltf2.textures)
//
//    // Merge the images of the two GLB files
//    gltf1.images.addAll(gltf2.images)
//
//    // Merge the accessors of the two GLB files
//    gltf1.accessors.addAll(gltf2.accessors)
//
//    // Return the merged GLB
//    return gltf1
//}
