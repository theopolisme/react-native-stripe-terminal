export const filterAndRenameObj = (object, startsWith)=>{
    return Object.keys(object).filter((key)=>key.startsWith(startsWith)).reduce((result, key)=>({
        ...result, [key.substring(startsWith.length).toUpperCase()]: object[key]
    }), {});
}